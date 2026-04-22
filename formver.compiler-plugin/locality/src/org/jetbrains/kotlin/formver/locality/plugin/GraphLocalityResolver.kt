/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

class GraphLocalityResolver(
    session: FirSession
) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> GraphLocalityResolver(session) }
        }
    }

    private val cachesFactory = session.firCachesFactory

    @OptIn(SymbolInternals::class)
    private val cache = cachesFactory.createCache { key: ControlFlowGraph, context: CheckerContext ->
        val flow = with(context) {
            key.analyzeLocality().getValue(key.exitNode)
        }

        flow.collapse()
    }

    context(context: CheckerContext)
    fun resolveInfoOf(graph: ControlFlowGraph): LocalityInfo =
        cache.getValue(graph, context)
}

val FirSession.localityInfoResolver: GraphLocalityResolver by FirSession.sessionComponentAccessor<GraphLocalityResolver>()

context(context: CheckerContext)
fun ControlFlowGraph.resolveLocalityInfo(): LocalityInfo =
    context.session.localityInfoResolver.resolveInfoOf(this)

@OptIn(SymbolInternals::class)
context(context: CheckerContext)
fun FirExpression.resolveLocality(): Locality {
    val expression = unwrapExpression()
    val immediateLocality = expression.resolveDirectLocality()

    if (immediateLocality != null) {
        return immediateLocality
    } else {
        val symbol = context.findClosest<FirCallableSymbol<*>>()
        val declaration = symbol?.fir
        val graph = (declaration as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph
            ?: return Global
        val facts = graph.resolveLocalityInfo()

        return facts[expression] ?: Global
    }
}
