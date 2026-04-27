/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType

private inline fun FirTypeRef.resolveLocality(findOwner: () -> FirBasedSymbol<*>?): Locality {
    if (coneType.attributes.locality == null) return Global

    return Local(findOwner())
}

private fun CheckerContext.findClosestFunction(): FirBasedSymbol<*>? =
    findClosest<FirFunctionSymbol<*>>()

/**
 * Resolves the locality required for the receiver parameter of a function upon its invocation.
 */
context(context: CheckerContext)
fun FirReceiverParameter.resolveRequiredLocality(): Locality =
    typeRef.resolveLocality { context.findClosestFunction() }

/**
 * Resolves the actual locality of a receiver parameter usage.
 */
fun FirReceiverParameter.resolveActualLocality(): Locality =
    typeRef.resolveLocality { containingDeclarationSymbol }

/**
 * Resolves the locality required for a value parameter of a function upon its invocation.
 */
context(context: CheckerContext)
fun FirValueParameter.resolveRequiredLocality(): Locality =
    returnTypeRef.resolveLocality { context.findClosestFunction() }

/**
 * Resolves the actual locality of a value parameter usage.
 */
fun FirValueParameter.resolveActualLocality(): Locality =
    returnTypeRef.resolveLocality { containingDeclarationSymbol }

/**
 * Resolves the invariant locality of a local property.
 */
context(context: CheckerContext)
fun FirProperty.resolveInvariantLocality(): Locality =
    returnTypeRef.resolveLocality { resolveOwner() }
