/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter

/**
 * Resolves the actual locality of a receiver parameter usage.
 */
fun FirReceiverParameter.resolveLocality(): Locality =
    typeRef.resolveLocality { containingDeclarationSymbol }

/**
 * Resolves the actual locality of a value parameter usage.
 */
fun FirValueParameter.resolveLocality(): Locality =
    returnTypeRef.resolveLocality { containingDeclarationSymbol }

/**
 * Resolves the invariant locality of a local property.
 */
context(context: CheckerContext)
fun FirProperty.resolveLocality(): Locality =
    returnTypeRef.resolveLocality { resolveOwner() }
