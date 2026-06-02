/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.util.previousCfgNodes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph

typealias UniquenessStatePair = Pair<UniquenessState, UniquenessState>

typealias StatementUniquenessStateMapping = Map<FirStatement, UniquenessStatePair>

class GraphUniquenessStateMappingResolver(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory =
            Factory { session -> GraphUniquenessStateMappingResolver(session) }
    }

    private val cache = session.firCachesFactory.createCache { graph: ControlFlowGraph, context: CheckerContext ->
        buildMapping(graph, context)
    }

    context(context: CheckerContext)
    fun resolveMappingOf(graph: ControlFlowGraph): StatementUniquenessStateMapping =
        cache.getValue(graph, context)

    private fun buildMapping(graph: ControlFlowGraph, context: CheckerContext): StatementUniquenessStateMapping {
        val outputs = context(context) { graph.resolveUniquenessStates() }
        val mapping = mutableMapOf<FirStatement, UniquenessStatePair>()

        fun CFGNode<*>.inputState(): UniquenessState =
            previousCfgNodes
                .map { predecessor -> outputs[predecessor].asUniquenessState() }
                .reduceOrNull(UniquenessState::join)
                ?: EmptyUniquenessState

        fun CFGNode<*>.outputState(): UniquenessState =
            outputs[this].asUniquenessState()

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

private val FirSession.graphUniquenessStateMappingResolver: GraphUniquenessStateMappingResolver
    by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun ControlFlowGraph.resolveUniquenessStateMapping(): StatementUniquenessStateMapping =
    context.session.graphUniquenessStateMappingResolver.resolveMappingOf(this)
