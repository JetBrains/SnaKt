package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.formver.core.conversion.Path
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.PredicateAccessPermissions
import org.jetbrains.kotlin.formver.core.embeddings.expression.While
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeInvariantEmbedding
import org.jetbrains.kotlin.formver.uniqueness.BorrowLevel
import org.jetbrains.kotlin.formver.uniqueness.UniqueLevel
import org.jetbrains.kotlin.formver.uniqueness.UniquenessTrie
import org.jetbrains.kotlin.formver.uniqueness.UniquenessType
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.formver.uniqueness.Path as FirPath

fun UniquenessType.isMoved(): Boolean = this == UniquenessType.Moved
fun UniquenessType.isShared(): Boolean = this is UniquenessType.Active && uniqueLevel == UniqueLevel.Shared
fun UniquenessType.isUnique(): Boolean = this is UniquenessType.Active && uniqueLevel == UniqueLevel.Unique
fun UniquenessType.isGlobal(): Boolean = this is UniquenessType.Active && borrowLevel == BorrowLevel.Global
fun UniquenessType.isLocal(): Boolean = this is UniquenessType.Active && borrowLevel == BorrowLevel.Local


class PermissionManager(val exp: ExpEmbedding) {


    val toUnfold: List<Path>
    val toFold: List<Path>

    init {
        if (exp.uniquenessBefore == null || exp.uniquenessAfter == null) {
            toUnfold = emptyList()
            toFold = emptyList()

        } else {
            val usedPaths = exp.containingPaths.value

            val pathsToConsider = usedPaths.mapNotNull { it.pathWithoutLast() }.toSet()

            val toUnfold = mutableListOf<Path>()
            val toFold = mutableListOf<Path>()

            for (path in pathsToConsider) {

                val childrenTypeBefore = exp.uniquenessBefore!!.ensure(path.firPath).childrenJoin
                val parentsTypeBefore = exp.uniquenessBefore!!.ensure(path.firPath).parentsJoin

                // if partially moved: continue (already unfolded)
                if (childrenTypeBefore.isMoved()) continue

                // it is unique, so we need to unfold
                if (parentsTypeBefore.isUnique()) {
                    toUnfold.add(path)
                }
            }

            for (path in pathsToConsider) {
                val childrenTypeAfter = exp.uniquenessAfter!!.ensure(path.firPath).childrenJoin
                val parentsTypeAfter = exp.uniquenessAfter!!.ensure(path.firPath).parentsJoin
                // is is moved
                if (childrenTypeAfter.isMoved()) continue

                if (parentsTypeAfter.isUnique()) {
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
    }

    private fun toUniquePredicates(path: Path, ctx: LinearizationContext): Exp.PredicateAccess {
        val predicate = path.type.uniquePredicateAccessInvariant()!!
        return buildPredicate(path.expEmbedding, predicate, ctx)
    }

    private fun toSharedPredicates(path: Path, ctx: LinearizationContext): Exp.PredicateAccess {
        val predicate = path.type.sharedPredicateAccessInvariant()!!
        return buildPredicate(path.expEmbedding, predicate, ctx)
    }

    private fun toSharedPredicates(unfoldTarget: ExpEmbedding, ctx: LinearizationContext): Exp.PredicateAccess {
        val predicate = unfoldTarget.type.sharedPredicateAccessInvariant()!!
        predicate.fillHole(unfoldTarget)
        return buildPredicate(unfoldTarget, predicate, ctx)
    }

    private fun buildPredicate(
        unfoldTarget: ExpEmbedding,
        predicate: TypeInvariantEmbedding,
        ctx: LinearizationContext
    ): Exp.PredicateAccess {
        val predicateApplied = (predicate.fillHole(unfoldTarget) as? PredicateAccessPermissions)
        val viperPredicate = predicateApplied?.toViperBuiltinType(ctx) as Exp.PredicateAccess
        return viperPredicate
    }

    fun getUniqueUnfolds(ctx: LinearizationContext): List<Exp.PredicateAccess> =
        toUnfold.map { toUniquePredicates(it, ctx) }

    fun getUniqueFolds(ctx: LinearizationContext): List<Exp.PredicateAccess> = toFold.map {
        toUniquePredicates(it, ctx)
    }
    fun isUnique(path: FirPath, trie: UniquenessTrie): Boolean = trie.ensure(path).parentsJoin.isUnique()
    fun isShared(path: FirPath, trie: UniquenessTrie): Boolean = trie.ensure(path).parentsJoin.isShared()
    fun isUnique(path: Path, trie: UniquenessTrie): Boolean = isUnique(path.firPath, trie)
    fun isShared(path: Path, trie: UniquenessTrie): Boolean = isShared(path.firPath, trie)
    fun isUnique(path: Path): Boolean = isUnique(path, exp.uniquenessAfter!!)
    fun isShared(path: Path): Boolean = isShared(path, exp.uniquenessAfter!!)

    fun isShared(): Boolean {
        val path = exp.endingPath.value!!
        val pathToConsider = path.pathWithoutLast()!!
        return isShared(pathToConsider)
    }

    fun addUniqueUnfolds(ctx: LinearizationContext) =
        getUniqueUnfolds(ctx).forEach { ctx.addStatement { Stmt.Unfold(it) } }

    fun addUniqueFolds(ctx: LinearizationContext) = getUniqueFolds(ctx).forEach { ctx.addStatement { Stmt.Fold(it) } }

    fun addSharedUnfold(ctx: LinearizationContext, unfoldTarget: ExpEmbedding) {
        if (isShared()) {
            val predicate = toSharedPredicates(unfoldTarget, ctx)
            ctx.addStatement { Stmt.Unfold(predicate) }
        }
    }

    fun extractWhileInvariants(ctx: LinearizationContext): List<ExpEmbedding> {
        check(exp is While) { "Invariants can only be extracted from a while loop." }

        val paths = exp.containingPaths.value
        val tries = listOf(
            exp.uniquenessBefore!!,
            exp.uniquenessAfter!!,
            exp.body.uniquenessAfter!!,
            exp.body.uniquenessBefore!!
        )

        fun isUnique(path: Path) = tries.all { isUnique(path, it) }
        fun isMoved(path: Path) = tries.all { it.ensure(path.firPath).childrenJoin.isMoved() }
        fun isShared(path: Path) = tries.all { isShared(path, it) }
        val resultUnique = mutableSetOf<Path>()
        val resultShared = mutableSetOf<Path>()
        for (path in paths) {

            for (prefix in path.traverse()) {

                if (isMoved(prefix)) continue
                if (isUnique(prefix)) {
                    resultUnique.add(prefix)
                    break
                }

                if (isShared(prefix)) {
                    resultShared.add(prefix)
                }
            }

        }

        val uniquePredicates = resultUnique.mapNotNull {
            val predicate = it.type.uniquePredicateAccessInvariant()
            predicate?.fillHole(it.expEmbedding)
        }
        val sharedPredicates = resultShared.mapNotNull {
            val predicate = it.type.sharedPredicateAccessInvariant()
            predicate?.fillHole(it.expEmbedding)
        }
        return uniquePredicates + sharedPredicates
    }
}