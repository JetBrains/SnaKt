/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.util.traverseToFixedPoint
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.formver.locality.plugin.CallParametersLocalityResolver


class GraphUniquenessStateFactsResolver(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> GraphUniquenessStateFactsResolver(session) }
        }
    }

    private val cache = session.firCachesFactory.createCache { graph: ControlFlowGraph, context: CheckerContext ->
        analyzeUniquenessStatesOf(graph, context)
    }

    fun resolveUniquenessStatesOf(
        graph: ControlFlowGraph,
        context: CheckerContext
    ): Map<CFGNode<*>, PathAwareUniquenessStateFacts> =
        cache.getValue(graph, context)

    private fun analyzeUniquenessStatesOf(
        graph: ControlFlowGraph,
        context: CheckerContext
    ): Map<CFGNode<*>, PathAwareUniquenessStateFacts> {
        val declaration = graph.declaration
        var initialState = EmptyUniquenessState

        if (declaration is FirFunction) {
            for (valueParameter in declaration.valueParameters) {
                val valueParameterSymbol = valueParameter.symbol

                initialState = context(context) {
                    initialState.associate(
                        valueParameterSymbol,
                        UniquenessState(valueParameterSymbol.resolveUniqueness())
                    )
                }
            }
        }

        val analyzer = GraphUniquenessStatesAnalyzer(
            initialState,
            context,
            CallParametersLocalityResolver
        )

        return graph.traverseToFixedPoint(analyzer)
    }
}

private val FirSession.graphUniquenessStateFactsResolver: GraphUniquenessStateFactsResolver
        by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun ControlFlowGraph.analyzeUniquenessStateFacts(): Map<CFGNode<*>, PathAwareUniquenessStateFacts> =
    context.session.graphUniquenessStateFactsResolver.resolveUniquenessStatesOf(this, context)
