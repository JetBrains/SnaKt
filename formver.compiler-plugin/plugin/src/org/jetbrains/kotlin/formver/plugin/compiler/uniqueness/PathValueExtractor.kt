package org.jetbrains.kotlin.formver.plugin.compiler.uniqueness

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.TailValueExtractor

abstract class PathValueExtractor<T> : TailValueExtractor<T, UniquenessTrie?>() {
    abstract fun visitReceiverExpression(
        symbol: FirBasedSymbol<*>?,
        explicitReceiver: FirExpression?,
        dispatchReceiver: FirExpression?,
        data: UniquenessTrie?
    ): T

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: UniquenessTrie?
    ): T {
        return visitReceiverExpression(
            symbol = propertyAccessExpression.calleeReference.symbol,
            explicitReceiver = propertyAccessExpression.explicitReceiver,
            dispatchReceiver = propertyAccessExpression.dispatchReceiver,
            data = data
        )
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: UniquenessTrie?
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
        data: UniquenessTrie?
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