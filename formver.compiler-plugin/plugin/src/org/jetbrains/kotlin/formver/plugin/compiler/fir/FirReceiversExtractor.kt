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

class FirReceiversExtractor(
    val cache: WeakHashMap<FirElement, Sequence<FirExpression>>
) : FirVisitor<Sequence<FirExpression>, Unit>() {
    private fun FirExpression?.visit(): Sequence<FirExpression> {
        return this?.accept(this@FirReceiversExtractor, Unit)
            ?: emptySequence()
    }

    fun extract(expr: FirExpression): Sequence<FirExpression> =
        expr.visit()

    override fun visitElement(
        element: FirElement,
        data: Unit
    ): Sequence<FirExpression> {
        return if (element is FirExpression) {
            sequenceOf(element)
        } else {
            emptySequence()
        }
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Unit
    ): Sequence<FirExpression> {
        val receivers = qualifiedAccessExpression.explicitReceiver.visit()

        return receivers + sequenceOf(qualifiedAccessExpression)
    }

    override fun visitSmartCastExpression(
        smartCastExpression: FirSmartCastExpression,
        data: Unit
    ): Sequence<FirExpression> {
        return smartCastExpression.originalExpression.visit()
    }

    private fun FirTypeOperatorCall.isCast(): Boolean =
        operation == FirOperation.AS || operation == FirOperation.SAFE_AS

    override fun visitTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: Unit
    ): Sequence<FirExpression> {
        if (!typeOperatorCall.isCast()) return sequenceOf(typeOperatorCall)

        return typeOperatorCall.argumentList.arguments.singleOrNull().visit()
    }

    override fun visitWrappedExpression(
        wrappedExpression: FirWrappedExpression,
        data: Unit
    ): Sequence<FirExpression> {
        return wrappedExpression.expression.visit()
    }

    override fun visitCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: Unit
    ): Sequence<FirExpression> {
        return checkNotNullCall.argumentList.arguments.first().visit()
    }

    override fun visitSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: Unit
    ): Sequence<FirExpression> {
        val receivers = safeCallExpression.receiver.visit()

        return receivers + sequenceOf(safeCallExpression)
    }
}