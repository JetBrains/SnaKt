package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.conversion.Path
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.PredicateAccessPermissions
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeInvariantEmbedding
import org.jetbrains.kotlin.formver.uniqueness.BorrowLevel
import org.jetbrains.kotlin.formver.uniqueness.UniqueLevel
import org.jetbrains.kotlin.formver.uniqueness.UniquenessType
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt

fun UniquenessType.isMoved(): Boolean = this == UniquenessType.Moved
fun UniquenessType.isShared(): Boolean = this is UniquenessType.Active && uniqueLevel == UniqueLevel.Shared
fun UniquenessType.isUnique(): Boolean = this is UniquenessType.Active && uniqueLevel == UniqueLevel.Unique
fun UniquenessType.isGlobal(): Boolean = this is UniquenessType.Active && borrowLevel == BorrowLevel.Global
fun UniquenessType.isLocal(): Boolean = this is UniquenessType.Active && borrowLevel == BorrowLevel.Local


class PermissionManager(val exp: ExpEmbedding) {

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

    fun getUniqueUnfolds(ctx: LinearizationContext): List<Exp.PredicateAccess> {
        if (exp.uniquenessBefore == null || exp.uniquenessAfter == null) throw SnaktInternalException(
            null,
            "No Uniqueness information provided"
        )


        val usedPaths = exp.containingPaths.value
        val toUnfold = mutableListOf<Path>()
        for (path in usedPaths) {
            val prefix = path.pathWithoutLast() ?: continue

            val childrenType = exp.uniquenessBefore!!.ensure(prefix.firPath).childrenJoin
            val parentsType = exp.uniquenessAfter!!.ensure(prefix.firPath).parentsJoin

            // if partially moved: continue (already unfolded)
            if (childrenType.isMoved()) continue

            // it is unique, so we need to unfold
            if (parentsType.isUnique()) {
                toUnfold.add(prefix)
            }
        }
        // We need to unfold a first before a.field
        toUnfold.sortBy { it.length }

        return toUnfold.map { toUniquePredicates(it, ctx) }
    }

    fun isUnique(path: Path): Boolean = exp.uniquenessAfter!!.ensure(path.firPath).parentsJoin.isUnique()
    fun isShared(path: Path): Boolean = exp.uniquenessAfter!!.ensure(path.firPath).parentsJoin.isShared()

    fun isShared(): Boolean {
        val path = exp.endingPath.value!!
        val pathToConsider = path.pathWithoutLast()!!
        return isShared(pathToConsider)
    }

    fun addUniqueUnfolds(ctx: LinearizationContext) {
        getUniqueUnfolds(ctx).forEach { ctx.addStatement { Stmt.Unfold(it) } }
    }


    fun addSharedUnfold(ctx: LinearizationContext) {
        val usedPaths = exp.containingPaths.value
        val path = usedPaths.first()
        val pathToConsider = path.pathWithoutLast()!!

        if (isShared(pathToConsider)) {
            val predicate = toSharedPredicates(pathToConsider, ctx)
            ctx.addStatement { Stmt.Unfold(predicate) }
        }
    }

    fun addSharedUnfold(ctx: LinearizationContext, unfoldTarget: ExpEmbedding) {
        if (isShared()) {
            val predicate = toSharedPredicates(unfoldTarget, ctx)
            ctx.addStatement { Stmt.Unfold(predicate) }
        }
    }

}