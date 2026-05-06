/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph

class GraphLocalityContractFactsResolver(
    session: FirSession
) : CacheSessionComponent<ControlFlowGraph, LocalityContractFacts, CheckerContext>(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> GraphLocalityContractFactsResolver(session) }
        }
    }

    override fun compute(key: ControlFlowGraph, context: CheckerContext): LocalityContractFacts {
        val flow = with(context) {
            key.analyzeLocalityContractFacts().getValue(key.exitNode)
        }

        return flow.collapse(LocalityContract::join)
    }

    /**
     * Resolve the [LocalityContractFacts] of a [graph], caching the result on the given [graph].
     */
    context(context: CheckerContext)
    fun resolveLocalityContractFactsOf(graph: ControlFlowGraph): LocalityContractFacts =
        getValue(graph, context)
}

val FirSession.graphLocalityContractFactsResolver: GraphLocalityContractFactsResolver
        by FirSession.sessionComponentAccessor<GraphLocalityContractFactsResolver>()

context(context: CheckerContext)
fun ControlFlowGraph.resolveLocalityContractFacts(): LocalityContractFacts =
    context.session.graphLocalityContractFactsResolver.resolveLocalityContractFactsOf(this)
