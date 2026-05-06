/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

/**
 * Resolves the locality contract of `this` expression based on the resolved locality contract info of the enclosing declaration.
 */
@OptIn(SymbolInternals::class)
context(context: CheckerContext)
fun FirExpression.resolveLocalityContract(): LocalityContract {
    val expression = unwrapExpression()
    val immediateContract = expression.resolveImmediateLocalityContract()
    val symbol = context.findClosest<FirCallableSymbol<*>>()
    val declaration = symbol?.fir
    val graph = (declaration as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph
        ?: return immediateContract
    val facts = with(context.session) {
        graph.resolveLocalityContractFacts()
    }

    return facts[expression] ?: immediateContract
}
