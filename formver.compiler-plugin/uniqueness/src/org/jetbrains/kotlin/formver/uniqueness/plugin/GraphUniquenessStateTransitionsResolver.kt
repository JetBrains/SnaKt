/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.util.previousCfgNodes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph

typealias UniquenessStateTransition = Pair<UniquenessState, UniquenessState>

class GraphUniquenessStateTransitionsResolver(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> GraphUniquenessStateTransitionsResolver(session) }
        }
    }

    private val cache = session.firCachesFactory.createCache { graph: ControlFlowGraph, context: CheckerContext ->
        with (context) {
            extractUniquenessStateTransitionsOf(graph)
        }
    }

    context(context: CheckerContext)
    fun resolveUniquenessStateTransitionsOf(graph: ControlFlowGraph): Map<FirStatement, UniquenessStateTransition> =
        cache.getValue(graph, context)

    context(context: CheckerContext)
    fun extractUniquenessStateTransitionsOf(
        graph: ControlFlowGraph,
    ): Map<FirStatement, UniquenessStateTransition> {
        val outputs = context(context) { graph.analyzeUniquenessStateFacts() }
        val mapping = mutableMapOf<FirStatement, UniquenessStateTransition>()

        fun CFGNode<*>.inputState(): UniquenessState =
            previousCfgNodes
                .map { predecessor -> outputs[predecessor].joinOverEdgeKinds() }
                .reduceOrNull(UniquenessState::join)
                ?: EmptyUniquenessState

        fun CFGNode<*>.outputState(): UniquenessState =
            outputs[this].joinOverEdgeKinds()

        // graph.nodes is already in topological (BFS) order: each node appears after all its
        // non-back-edge predecessors.
        for (node in graph.nodes) {
            val element = node.fir

            if (element is FirStatement) {
                mapping.putIfAbsent(
                    element,
                    node.inputState() to node.outputState()
                )
            }
        }

        return mapping
    }
}

private val FirSession.graphUniquenessStateTransitionsResolver: GraphUniquenessStateTransitionsResolver
    by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun ControlFlowGraph.resolveUniquenessStateMapping():  Map<FirStatement, UniquenessStateTransition> =
    context.session.graphUniquenessStateTransitionsResolver.resolveUniquenessStateTransitionsOf(this)
