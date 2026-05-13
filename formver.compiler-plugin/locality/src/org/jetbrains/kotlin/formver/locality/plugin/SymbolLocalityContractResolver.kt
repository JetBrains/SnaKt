/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType

context(context: CheckerContext)
fun FirVariableSymbol<*>.resolveLocalityContract(): LocalityContract {
    if (resolvedReturnType is ConeErrorType) return null

    if (resolvedReturnTypeRef.source?.kind !is KtFakeSourceElementKind.ImplicitTypeRef) {
        return resolvedReturnType.resolveLocalityContract(context.session)
    }

    return resolvedInitializer?.resolveLocalityContract()
        ?: resolvedReturnType.resolveLocalityContract(context.session)
}

context(context: CheckerContext)
fun FirCallableSymbol<*>.resolveLocalityContract(): LocalityContract =
    resolvedReturnType.resolveLocalityContract(context.session)

context(context: CheckerContext)
fun FirReceiverParameterSymbol.resolveLocalityContract(): LocalityContract =
    resolvedType.resolveLocalityContract(context.session)
