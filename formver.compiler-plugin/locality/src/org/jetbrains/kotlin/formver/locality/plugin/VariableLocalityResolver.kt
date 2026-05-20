/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef

private fun FirTypeRef.isImplicit(): Boolean =
    source?.kind == KtFakeSourceElementKind.ImplicitTypeRef

/**
 * Resolves the locality of a variable symbol.
 *
 * If the locality of a variable is explicitly specified, it is returned directly. Otherwise it is inferred from the
 * initializer. For example:
 * val x: local Any = ... // x is assumed to be local
 * val x: Any = ... // x is assumed to be global
 * val x = ... // locality of x is inferred from the initializer
 *
 * Note that, for the last case, solely relying on the `LocalityAttribute` of `x` is not enough, as the attribute may be
 * overridden by a cast.
 */
context(context: CheckerContext)
fun FirVariableSymbol<*>.resolveLocality(): Locality {
    if (!resolvedReturnTypeRef.isImplicit()) return resolvedReturnTypeRef.coneType.locality

    val typeLocality = resolvedReturnType.locality

    return resolvedInitializer?.resolveLocality() ?: typeLocality
}
