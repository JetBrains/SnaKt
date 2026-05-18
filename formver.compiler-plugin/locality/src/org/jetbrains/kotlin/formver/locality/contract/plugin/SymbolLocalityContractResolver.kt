/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.contract.plugin

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.varargElementType
import org.jetbrains.kotlin.formver.type.plugin.SymbolTypeResolver

context(context: CheckerContext)
fun FirReceiverParameterSymbol.resolveLocalityContract(): LocalityContract =
    resolvedType.resolveLocalityContract(context.session)

object ReceiverLocalityContractResolver : SymbolTypeResolver<LocalityContract, FirReceiverParameterSymbol> {
    context(context: CheckerContext)
    override fun resolveTypeOf(symbol: FirReceiverParameterSymbol): LocalityContract =
        symbol.resolveLocalityContract()
}

context(context: CheckerContext)
fun FirCallableSymbol<*>.resolveLocalityContract(): LocalityContract =
    when (this) {
        is FirVariableSymbol<*> -> {
            if (resolvedReturnType is ConeErrorType) return null

            val returnType = if (this is FirValueParameterSymbol) {
                resolvedReturnType.varargElementType()
            } else {
                resolvedReturnType
            }

            if (resolvedReturnTypeRef.source?.kind !is KtFakeSourceElementKind.ImplicitTypeRef) {
                return returnType.resolveLocalityContract(context.session)
            }

            return resolvedInitializer?.resolveLocalityContract()
                ?: returnType.resolveLocalityContract(context.session)
        }
        else -> resolvedReturnType.resolveLocalityContract(context.session)
    }

object VariableLocalityContractResolver : SymbolTypeResolver<LocalityContract, FirVariableSymbol<*>> {
    context(context: CheckerContext)
    override fun resolveTypeOf(symbol: FirVariableSymbol<*>): LocalityContract =
        symbol.resolveLocalityContract()
}
