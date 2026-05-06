/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph

class GraphLocalityContractFactsResolver(
    session: FirSession
) : CacheSessionComponent<ControlFlowGraph, LocalityContractFacts, Unit>(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> GraphLocalityContractFactsResolver(session) }
        }
    }

    override fun compute(key: ControlFlowGraph, context: Unit): LocalityContractFacts {
        val flow = key.analyzeLocalityContractFacts().getValue(key.exitNode)
        return flow.collapse(LocalityContract::union)
    }

    /**
     * Resolve the [LocalityContractFacts] of a [graph], caching the result on the given [graph].
     */
    fun resolveLocalityContractFactsOf(graph: ControlFlowGraph): LocalityContractFacts =
        getValue(graph, Unit)
}

val FirSession.graphLocalityContractFactsResolver: GraphLocalityContractFactsResolver
        by FirSession.sessionComponentAccessor<GraphLocalityContractFactsResolver>()

context(session: FirSession)
fun ControlFlowGraph.resolveLocalityContractFacts(): LocalityContractFacts =
    session.graphLocalityContractFactsResolver.resolveLocalityContractFactsOf(this)
