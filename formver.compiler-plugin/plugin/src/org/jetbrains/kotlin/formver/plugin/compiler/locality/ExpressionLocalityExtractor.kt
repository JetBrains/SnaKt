package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
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

    @OptIn(SymbolInternals::class)
    override fun FirBasedSymbol<*>.extract(): Locality {
        when (this) {
            is FirVariableSymbol<*> -> {
                context(context) {
                    return fir.usageLocality
                }
            }
            else -> return Locality.Global
        }
    }
}

/**
 * Extracts the locality of [this] expression with respect to the outer declarations.
 */
context(context: CheckerContext)
val FirExpression.resolvedLocality: Locality
    get() = ExpressionLocalityExtractor(context).extract(this)
