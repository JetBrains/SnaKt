/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

/**
 * Resolves `this` access expression's locality.
 */
@OptIn(SymbolInternals::class)
context(context: CheckerContext)
fun FirQualifiedAccessExpression.resolveImmediateLocality(): Locality =
    when (this) {
        is FirThisReceiverExpression -> {
            when (val boundSymbol = calleeReference.boundSymbol) {
                is FirReceiverParameterSymbol -> boundSymbol.fir.resolveActualLocality()
                else -> Locality.Global
            }
        }
        is FirPropertyAccessExpression -> {
            when (val calleeSymbol = calleeReference.symbol) {
                is FirVariableSymbol<*> ->
                    when (val declaration = calleeSymbol.fir) {
                        is FirValueParameter -> declaration.resolveActualLocality()
                        is FirProperty -> declaration.resolveInvariantLocality()
                        else -> Locality.Global
                    }
                else -> Locality.Global
            }
        }
        else -> Locality.Global
    }
