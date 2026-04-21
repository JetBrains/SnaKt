package org.jetbrains.kotlin.formver.analysis

import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirCheckedSafeCallSubject
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirWrappedExpression

/**
 * Path-transparent refinement of [ExpressionTailFolder].
 *
 * The folder treats a wrapper as semantically equivalent to its wrapped expression and continues folding through it.
 * For example:
 * - `x as T` / `x as? T` -> folds `x`
 * - `x!!` -> folds `x`
 *
 * Subclasses can still override any visitor method to impose analysis-specific policy
 * (for example, treating some safe-call selector kinds conservatively).
 *
 * [T] is the folded domain and [D] is the visitor data threaded through traversal.
 */
abstract class ExpressionPathFolder<T, D> : ExpressionTailFolder<T, D>() {
    private fun FirTypeOperatorCall.isCast(): Boolean =
        operation == FirOperation.AS || operation == FirOperation.SAFE_AS

    override fun visitTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: D
    ): T {
        if (!typeOperatorCall.isCast()) return empty

        return typeOperatorCall.argumentList.arguments.singleOrNull().visit(data)
    }

    override fun visitCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: D
    ): T {
        return checkNotNullCall.argumentList.arguments.singleOrNull().visit(data)
    }

    override fun visitSmartCastExpression(
        smartCastExpression: FirSmartCastExpression,
        data: D
    ): T {
        return smartCastExpression.originalExpression.visit(data)
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: D
    ): T {
        return visitQualifiedAccessExpression(propertyAccessExpression, data)
    }

    override fun visitSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: D
    ): T {
        val selector = safeCallExpression.selector as? FirQualifiedAccessExpression ?: return empty

        return selector.visit(data)
    }

    override fun visitCheckedSafeCallSubject(
        checkedSafeCallSubject: FirCheckedSafeCallSubject,
        data: D
    ): T {
        return checkedSafeCallSubject.originalReceiverRef.value.visit(data)
    }

    override fun visitWrappedExpression(
        wrappedExpression: FirWrappedExpression,
        data: D
    ): T {
        return wrappedExpression.expression.visit(data)
    }
}
