/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph

class GraphExpressionLocalityFactsResolver(
    session: FirSession
) : CacheSessionComponent<ControlFlowGraph, LocalityFacts, CheckerContext>(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> GraphExpressionLocalityFactsResolver(session) }
        }
    }

    override fun compute(key: ControlFlowGraph, context: CheckerContext): LocalityFacts {
        val flow = with(context) {
            key.analyzeExpressionLocality().getValue(key.exitNode)
        }
        return flow.collapse(Locality::union)
    }

    /**
     * Resolve the [LocalityFacts] of a [graph], caching the result on the given [graph].
     */
    context(context: CheckerContext)
    fun resolveLocalityFactsOf(graph: ControlFlowGraph): LocalityFacts =
        getValue(graph, context)
}

val FirSession.graphExpressionLocalityFactsResolver: GraphExpressionLocalityFactsResolver
        by FirSession.sessionComponentAccessor<GraphExpressionLocalityFactsResolver>()

context(context: CheckerContext)
fun ControlFlowGraph.resolveLocalityFacts(): LocalityFacts =
    context.session.graphExpressionLocalityFactsResolver.resolveLocalityFactsOf(this)
