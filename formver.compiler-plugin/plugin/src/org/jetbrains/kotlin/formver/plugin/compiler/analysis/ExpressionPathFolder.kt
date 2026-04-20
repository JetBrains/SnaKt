package org.jetbrains.kotlin.formver.plugin.compiler.analysis

import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirCheckedSafeCallSubject
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall

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
}
