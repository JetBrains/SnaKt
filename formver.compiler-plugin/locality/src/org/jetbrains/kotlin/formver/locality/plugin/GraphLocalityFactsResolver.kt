/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

class GraphLocalityInfoResolver(
    session: FirSession
) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> GraphLocalityInfoResolver(session) }
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
     * Resolve the [LocalityFacts] of a [graph], caching the result on the given [graph].
     */
    context(context: CheckerContext)
    fun resolveLocalityFactsOf(graph: ControlFlowGraph): LocalityFacts =
        cache.getValue(graph, context)
}

val FirSession.graphLocalityInfoResolver: GraphLocalityInfoResolver
        by FirSession.sessionComponentAccessor<GraphLocalityInfoResolver>()

context(context: CheckerContext)
fun ControlFlowGraph.resolveLocalityFacts(): LocalityFacts =
    context.session.graphLocalityInfoResolver.resolveLocalityFactsOf(this)
