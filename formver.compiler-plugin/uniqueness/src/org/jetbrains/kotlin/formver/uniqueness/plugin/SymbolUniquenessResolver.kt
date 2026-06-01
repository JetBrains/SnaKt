/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.formver.type.plugin.SymbolTypeResolver

context(context: CheckerContext)
fun FirBasedSymbol<*>.resolveComponentUniqueness(): Uniqueness =
    when (this) {
        is FirVariableSymbol<*> -> resolveUniqueness()
        is FirReceiverParameterSymbol -> resolveUniqueness()
        else -> Uniqueness.Shared
    }

fun FirReceiverParameterSymbol.resolveUniqueness(): Uniqueness =
    resolvedType.defaultUniqueness

object ReceiverUniquenessResolver :
    SymbolTypeResolver<Uniqueness, FirReceiverParameterSymbol> {
    context(context: CheckerContext)
    override fun resolveTypeOf(symbol: FirReceiverParameterSymbol): Uniqueness =
        symbol.resolveUniqueness()
}

context(context: CheckerContext)
fun FirVariableSymbol<*>.resolveUniqueness(): Uniqueness {
    if (resolvedReturnType is ConeErrorType) return Uniqueness.Shared

    if (resolvedReturnTypeRef.source?.kind !is KtFakeSourceElementKind.ImplicitTypeRef) {
        return resolvedReturnType.defaultUniqueness
    }

    return resolvedInitializer?.resolveAccessState()?.extractDefaultUniqueness() ?: Uniqueness.Shared
}

object VariableUniquenessResolver :
    SymbolTypeResolver<Uniqueness, FirVariableSymbol<*>> {
    context(context: CheckerContext)
    override fun resolveTypeOf(symbol: FirVariableSymbol<*>): Uniqueness =
        symbol.resolveUniqueness()
}

