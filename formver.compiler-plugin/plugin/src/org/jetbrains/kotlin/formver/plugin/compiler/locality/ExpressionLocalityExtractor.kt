package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.TailValueExtractor

) : TailValueExtractor<Locality, Unit>() {
    fun extract(expression: FirExpression): Locality {
        return extract(expression, Unit)
    }

    private val context: CheckerContext
) : TailValueExtractor<Locality>() {
    override val empty = Locality.Global

    override fun Locality.join(other: Locality): Locality {
        return join(other)
    }

    @OptIn(SymbolInternals::class)
    fun FirBasedSymbol<*>.extract(): Locality {
        when (this) {
            is FirVariableSymbol<*> -> {
                context(context) {
                    return fir.usageLocality
                }
            }
            else -> return Locality.Global
        }
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: Unit
    ): Locality {
        return propertyAccessExpression.explicitReceiver?.visit(data)
            ?: propertyAccessExpression.calleeReference.symbol?.extract() ?: empty
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Unit
    ): Locality {
        return qualifiedAccessExpression.explicitReceiver.visit(data)
    }

    override fun visitSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: Unit
    ): Locality {
        return safeCallExpression.receiver.visit(data)
    }
}

/**
 * Extracts the locality of [this] expression with respect to the outer declarations.
 */
context(context: CheckerContext)
val FirExpression.resolvedLocality: Locality
    get() = ExpressionLocalityExtractor(context).extract(this)
