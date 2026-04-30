/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

/**
 * Resolves the locality of `this` expression based on the resolved locality info of the enclosing declaration.
 */
@OptIn(SymbolInternals::class)
context(context: CheckerContext)
fun FirExpression.resolveLocality(): Locality {
    val expression = unwrapExpression()

    if (expression is FirQualifiedAccessExpression) {
        return expression.resolveImmediateLocality()
    } else {
        val symbol = context.findClosest<FirCallableSymbol<*>>()
        val declaration = symbol?.fir
        val graph = (declaration as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph
            ?: return Locality.Global
        val facts = graph.resolveLocalityFacts()

        return facts[expression] ?: Locality.Global
    }
}
