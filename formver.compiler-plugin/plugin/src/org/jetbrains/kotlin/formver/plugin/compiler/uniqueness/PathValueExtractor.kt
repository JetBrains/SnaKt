package org.jetbrains.kotlin.formver.plugin.compiler.uniqueness

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.TailValueExtractor

abstract class PathValueExtractor<T, D> : TailValueExtractor<T, D>() {
    abstract fun visitReceiverExpression(
        symbol: FirBasedSymbol<*>?,
        explicitReceiver: FirExpression?,
        dispatchReceiver: FirExpression?,
        data: D
    ): T

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: D
    ): T {
        return visitQualifiedAccessExpression(propertyAccessExpression, data)
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: D
    ): T {
        return visitReceiverExpression(
            symbol = qualifiedAccessExpression.calleeReference.symbol,
            explicitReceiver = qualifiedAccessExpression.explicitReceiver,
            dispatchReceiver = qualifiedAccessExpression.dispatchReceiver,
            data = data
        )
    }

    override fun visitSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: D
    ): T {
        val selectorSymbol = (safeCallExpression.selector as? FirQualifiedAccessExpression)?.calleeReference?.symbol

        return visitReceiverExpression(
            symbol = selectorSymbol,
            explicitReceiver = safeCallExpression.receiver,
            dispatchReceiver = null,
            data = data
        )
    }
}
