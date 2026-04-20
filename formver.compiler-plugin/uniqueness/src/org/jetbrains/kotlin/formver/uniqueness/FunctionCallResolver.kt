package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

@OptIn(SymbolInternals::class)
internal fun FirFunctionCall.resolveFunction(): FirFunction? {
    val callableSymbol = toResolvedCallableSymbol()
        ?: error("Unresolved callable symbol for function call: $this")

    return callableSymbol.fir as? FirFunction
        ?: error("Expected function callee for $this, got ${callableSymbol::class.simpleName}")
}
