package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.formver.core.conversion.Path
import org.jetbrains.kotlin.formver.core.conversion.PathElement
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.PredicateAccessPermissions
import org.jetbrains.kotlin.formver.core.embeddings.expression.While
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeInvariantEmbedding
import org.jetbrains.kotlin.formver.uniqueness.UniqueLevel
import org.jetbrains.kotlin.formver.uniqueness.UniquenessTrie
import org.jetbrains.kotlin.formver.uniqueness.UniquenessType
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.formver.uniqueness.Path as FirPath


sealed interface Helper {
    fun UniquenessType.isMoved(): Boolean = this == UniquenessType.Moved
    fun UniquenessType.isShared(): Boolean = this is UniquenessType.Active && uniqueLevel == UniqueLevel.Shared
    fun UniquenessType.isUnique(): Boolean = this is UniquenessType.Active && uniqueLevel == UniqueLevel.Unique
    fun isUnique(path: FirPath, trie: UniquenessTrie): Boolean = trie.ensure(path).parentsJoin.isUnique()
    fun isShared(path: FirPath, trie: UniquenessTrie): Boolean = trie.ensure(path).parentsJoin.isShared()
    fun isMoved(path: FirPath, trie: UniquenessTrie): Boolean = trie.ensure(path).childrenJoin.isMoved()
    fun isUnique(path: Path, trie: UniquenessTrie): Boolean = isUnique(path.firPath, trie)
    fun isShared(path: Path, trie: UniquenessTrie): Boolean = isShared(path.firPath, trie)
    fun isMoved(path: Path, trie: UniquenessTrie): Boolean = isMoved(path.firPath, trie)

}


sealed interface PredicateBuilder {
    fun buildPredicate(
        unfoldTarget: ExpEmbedding,
        predicate: TypeInvariantEmbedding,
        ctx: LinearizationContext
    ): Exp.PredicateAccess? {
        val predicateApplied = (predicate.fillHole(unfoldTarget) as? PredicateAccessPermissions)
        val viperPredicate = predicateApplied?.toViperBuiltinType(ctx) as? Exp.PredicateAccess
        return viperPredicate
    }

    fun buildUniquePredicate(
        path: Path,
        ctx: LinearizationContext
    ): Exp.PredicateAccess? =
        path.expEmbedding.type.uniquePredicateAccessInvariant()?.run { buildPredicate(path.expEmbedding, this, ctx) }

    fun buildSharedPredicate(
        path: Path,
        ctx: LinearizationContext
    ): Exp.PredicateAccess? =
        path.expEmbedding.type.sharedPredicateAccessInvariant()?.run { buildPredicate(path.expEmbedding, this, ctx) }

}

sealed interface Folder {
    fun unfold(ctx: LinearizationContext)
    fun fold(ctx: LinearizationContext)
}

/**
 * The `UniquePermissionManager` figures out which unfolds and folds are needed around a statement.
 * The object can only be created through the `create` function. The creation can fail (null is returned). This happens when
 * there are not enough information available to determine the necessary folds.
 *
 * There is a limited amount of `ExpEmbeddings` that can be used to create this object. There is not an exact definition,
 * but it should only be used with `ExpEmbeddings` which correspond to statements.
 */
class UniquePermissionManager private constructor(before: UniquenessTrie, after: UniquenessTrie, paths: Set<Path>) :
    PredicateBuilder, Folder, Helper {

    companion object {
        fun create(exp: ExpEmbedding): UniquePermissionManager? {
            val before = exp.uniquenessBefore ?: return null
            val after = exp.uniquenessAfter ?: return null
            val paths = exp.containingPaths.value
            return UniquePermissionManager(before, after, paths)
        }

    }


    val toUnfold: List<Path>
    val toFold: List<Path>

    init {
        val pathsToConsider = paths.mapNotNull { it.pathWithoutLast() }.toSet()
        val toUnfold = mutableListOf<Path>()
        val toFold = mutableListOf<Path>()

        for (path in pathsToConsider) {
            // if partially moved: continue (already unfolded)
            if (isMoved(path, before)) continue

            // it is unique, so we need to unfold
            if (isUnique(path, before)) {
                toUnfold.add(path)
            }
        }

        for (path in pathsToConsider) {
            // is is moved, we can not fold it back
            if (isMoved(path, after)) continue

            if (isUnique(path, after)) {
                toFold.add(path)
            }
        }
        // We need to unfold `first` before `a.field`
        toUnfold.sortBy { it.length }
        // We need to fold `a.field` before `first`
        toFold.sortByDescending { it.length }
        this.toUnfold = toUnfold
        this.toFold = toFold
    }

    fun uniquePredicates(list: List<Path>, ctx: LinearizationContext): List<Exp.PredicateAccess> =
        list.mapNotNull { buildUniquePredicate(it, ctx) }


    override fun unfold(ctx: LinearizationContext) {
        uniquePredicates(toUnfold, ctx).forEach { ctx.addStatement { Stmt.Unfold(it) } }
    }

    override fun fold(ctx: LinearizationContext) {
        uniquePredicates(toFold, ctx).forEach { ctx.addStatement { Stmt.Fold(it) } }
    }
}

/**
 * The `SharedPermissionManager` figures out which unfolds and folds are needed around an access.
 * The object can only be created through the `create` function. The creation can fail (null is returned). This happens when
 * there are not enough information available to determine the necessary folds.
 *
 */
class SharedPermissionManager(before: UniquenessTrie, path: PathElement) :
    PredicateBuilder,
    Folder, Helper {

    val prefixPath = path.pathWithoutLast()

    companion object {
        fun create(exp: ExpEmbedding): SharedPermissionManager? {
            val before = exp.uniquenessBefore ?: return null
            val usedPath = exp.endingPath.value ?: return null
            if (usedPath.length <= 1) return null
            return SharedPermissionManager(
                before,
                usedPath as PathElement
            ) //cast will always succeed, because of length > 1
        }
    }


    /**
     * True iff the last receiver is shared.
     * E.g. if tha path is x.y.z, it is true if y is shared
     */
    val isShared = isShared(prefixPath, before)


    fun predicate(path: Path, target: ExpEmbedding, ctx: LinearizationContext) =
        prefixPath.type.sharedPredicateAccessInvariant()?.run { buildPredicate(target, this, ctx) }

    override fun unfold(ctx: LinearizationContext) {
        buildSharedPredicate(prefixPath, ctx)?.let { ctx.addStatement { Stmt.Unfold(it) } }
    }

    /**
     * The predicate will be unfolded for the target.
     * This is used when previous access was translated into a havoc call. Hence, the path is broken and restarts
     * on the local variable (``target``)
     */
    fun unfold(ctx: LinearizationContext, target: ExpEmbedding) {
        predicate(prefixPath, target, ctx)?.let { ctx.addStatement { Stmt.Unfold(it) } }
    }

    override fun fold(ctx: LinearizationContext) {
        buildSharedPredicate(prefixPath, ctx)?.let { ctx.addStatement { Stmt.Fold(it) } }
    }

}

/**
 * The `WhilePermissionManager` figures out the necessary invariants for a while loop.
 * The object can only be created through the `create` function. The creation can fail (null is returned). This happens when
 * there are not enough information available to determine the necessary folds.
 *
 */
class WhilePermissionManager private constructor(
    val before: UniquenessTrie,
    val after: UniquenessTrie,
    val beforeBody: UniquenessTrie,
    val afterBody: UniquenessTrie,
    val paths: Set<Path>
) : PredicateBuilder, Helper {

    companion object {
        fun create(exp: While): WhilePermissionManager? {
            val before = exp.uniquenessBefore ?: return null
            val after = exp.uniquenessAfter ?: return null
            val beforeBody = exp.body.uniquenessBefore ?: return null
            val afterBody = exp.body.uniquenessAfter ?: return null
            val paths = exp.containingPaths.value
            return WhilePermissionManager(before, after, beforeBody, afterBody, paths)
        }
    }


    fun extractWhileInvariants(): List<ExpEmbedding> {

        val uniqueness = listOf(
            before,
            beforeBody,
            afterBody,
            after,
        )

        fun isUnique(path: FirPath) = uniqueness.all { isUnique(path, it) }
        fun isMoved(path: FirPath) = uniqueness.all { isMoved(path, it) }
        fun isShared(path: FirPath) = uniqueness.all { isShared(path, it) }

        val resultUnique = mutableSetOf<Path>()
        val resultShared = mutableSetOf<Path>()
        for (path in paths) {
            for (prefix in path.traverse()) {
                val firPrefix = prefix.firPath ?: continue

                if (isMoved(firPrefix)) continue
                if (isUnique(firPrefix)) {
                    resultUnique.add(prefix)
                    break
                }

                if (isShared(firPrefix)) {
                    resultShared.add(prefix)
                }
            }

        }

        val uniquePredicates =
            resultUnique.mapNotNull { it.expEmbedding.type.uniquePredicateAccessInvariant()?.fillHole(it.expEmbedding) }
        val sharedPredicates =
            resultShared.mapNotNull { it.expEmbedding.type.sharedPredicateAccessInvariant()?.fillHole(it.expEmbedding) }
        return uniquePredicates + sharedPredicates
    }


}