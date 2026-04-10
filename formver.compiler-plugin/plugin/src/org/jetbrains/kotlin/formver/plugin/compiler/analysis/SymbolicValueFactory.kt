package org.jetbrains.kotlin.formver.plugin.compiler.analysis

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

interface SymbolicValueFactory<T : SymbolicValue<T>> {
    fun fromSymbol(symbol: FirBasedSymbol<*>): T
}
