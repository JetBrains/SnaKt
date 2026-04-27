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

    /**
     * Resolve the [LocalityInfo] of a [graph], caching the result on the given [graph].
     */
    context(context: CheckerContext)
    fun resolveLocalityInfoOf(graph: ControlFlowGraph): LocalityInfo =
        cache.getValue(graph, context)
}

val FirSession.graphLocalityResolver: GraphLocalityResolver
        by FirSession.sessionComponentAccessor<GraphLocalityResolver>()

context(context: CheckerContext)
fun ControlFlowGraph.resolveLocality(): LocalityInfo =
    context.session.graphLocalityResolver.resolveLocalityInfoOf(this)

/**
 * Resolves the locality of `this` expression based on the resolved locality info of the enclosing declaration.
 */
@OptIn(SymbolInternals::class)
context(context: CheckerContext)
fun FirExpression.resolveLocality(): Locality {
    val expression = unwrapExpression()
    val immediateLocality = expression.resolveComponentLocality()

    if (immediateLocality != null) {
        return immediateLocality
    } else {
        val symbol = context.findClosest<FirCallableSymbol<*>>()
        val declaration = symbol?.fir
        val graph = (declaration as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph
            ?: return Global
        val facts = graph.resolveLocality()

        return facts[expression] ?: Global
    }
}
