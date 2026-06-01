/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.formver.type.plugin.SymbolTypeResolver

fun FirReceiverParameterSymbol.resolveLocality(): Locality =
    resolvedType.locality

object ReceiverLocalityResolver :
    SymbolTypeResolver<Locality, FirReceiverParameterSymbol> {
    context(context: CheckerContext)
    override fun resolveTypeOf(symbol: FirReceiverParameterSymbol): Locality =
        symbol.resolveLocality()
}

context(context: CheckerContext)
fun FirVariableSymbol<*>.resolveLocality(): Locality {
    if (resolvedReturnType is ConeErrorType) return null

    if (resolvedReturnTypeRef.source?.kind !is KtFakeSourceElementKind.ImplicitTypeRef) {
        return resolvedReturnType.locality
    }

    return resolvedInitializer?.resolveLocality()
}

object VariableLocalityResolver :
    SymbolTypeResolver<Locality, FirVariableSymbol<*>> {
    context(context: CheckerContext)
    override fun resolveTypeOf(symbol: FirVariableSymbol<*>): Locality =
        symbol.resolveLocality()
}
