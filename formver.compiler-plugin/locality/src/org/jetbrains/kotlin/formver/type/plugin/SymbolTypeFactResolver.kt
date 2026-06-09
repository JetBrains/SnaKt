/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

/**
 * Resolves the declared type fact of a symbol.
 */
fun interface SymbolTypeFactResolver<TypeFact, in Symbol : FirBasedSymbol<*>> {
    context(context: CheckerContext)
    fun resolveTypeFactOf(symbol: Symbol): TypeFact
}
