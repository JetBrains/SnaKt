package org.jetbrains.kotlin.formver.plugin.compiler.fir

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirWrappedExpression
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import java.util.WeakHashMap

/**
 * Extract the receiver of an expression
 */
class FirExpressionReceiverExtractor(
    private val cache: WeakHashMap<FirElement, FirExpression?>
) : FirVisitor<FirExpression?, Unit>() {
    private fun FirExpression?.visit(): FirExpression? {
        this ?: return null

        return cache.computeIfAbsent(this) {
            accept(this@FirExpressionReceiverExtractor, Unit)
        }
    }

    fun extract(expr: FirExpression): FirExpression? =
        expr.visit()

    override fun visitElement(
        element: FirElement,
        data: Unit
    ): FirExpression? {
        return null
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Unit
    ): FirExpression? {
        return qualifiedAccessExpression.explicitReceiver
    }

    override fun visitSmartCastExpression(
        smartCastExpression: FirSmartCastExpression,
        data: Unit
    ): FirExpression? {
        return smartCastExpression.originalExpression.visit()
    }

    private fun FirTypeOperatorCall.isCast(): Boolean =
        operation == FirOperation.AS || operation == FirOperation.SAFE_AS

    override fun visitTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: Unit
    ): FirExpression? {
        if (!typeOperatorCall.isCast()) return null

        return typeOperatorCall.argumentList.arguments.singleOrNull().visit()
    }

    override fun visitWrappedExpression(
        wrappedExpression: FirWrappedExpression,
        data: Unit
    ): FirExpression? {
        return wrappedExpression.expression.visit()
    }

    override fun visitCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: Unit
    ): FirExpression? {
        return checkNotNullCall.argumentList.arguments.first().visit()
    }

    override fun visitSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: Unit
    ): FirExpression? {
        return safeCallExpression.receiver.visit()
    }
}