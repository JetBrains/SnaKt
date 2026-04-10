package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.SymbolicValueFactory

class LocalityValueFactory(
    private val context: CheckerContext
) : SymbolicValueFactory<LocalityValue> {
    @OptIn(SymbolInternals::class)
    override fun fromSymbol(symbol: FirBasedSymbol<*>): LocalityValue {
        val element = symbol.fir

        with(context) {
            return when (element) {
                is FirVariable -> element.declaredLocality
                else -> LocalityValue.Global
            }
        }
    }
}
