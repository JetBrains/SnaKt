package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.TailValueExtractor

class ExpressionLocalityValueExtractor(
    private val context: CheckerContext
) : TailValueExtractor<Locality>() {
    override val empty = Locality.Global

    @OptIn(SymbolInternals::class)
    override fun FirBasedSymbol<*>.extract(): Locality {
        when (this) {
            is FirVariableSymbol<*> -> {
                context(context) {
                    return fir.actualLocality
                }
            }
            else -> return Locality.Global
        }
    }
}

context(context: CheckerContext)
val FirExpression.resolvedLocality: Locality
    get() = ExpressionLocalityValueExtractor(context).extract(this)
