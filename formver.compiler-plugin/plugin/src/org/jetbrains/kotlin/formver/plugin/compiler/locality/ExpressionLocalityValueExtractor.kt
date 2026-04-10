package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.SymbolicValueExtractor

class ExpressionLocalityValueExtractor(
    private val context: CheckerContext
) : SymbolicValueExtractor<LocalityValue>() {
    override val empty = LocalityValue.Global

    @OptIn(SymbolInternals::class)
    override fun FirBasedSymbol<*>.extract(): LocalityValue {
        when (this) {
            is FirVariableSymbol<*> -> {
                context(context) {
                    return fir.actualLocality
                }
            }
            else -> return LocalityValue.Global
        }
    }
}

context(context: CheckerContext)
val FirExpression.resolvedLocality: LocalityValue
    get() = ExpressionLocalityValueExtractor(context).extract(this)
