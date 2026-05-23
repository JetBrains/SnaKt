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
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.JumpNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ThrowExceptionNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode

typealias ExpressionUniquenessStateMapping = Map<FirExpression, UniquenessState>

class GraphUniquenessStateMappingResolver(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory =
            Factory { session -> GraphUniquenessStateMappingResolver(session) }
    }

    private val cache = session.firCachesFactory.createCache { graph: ControlFlowGraph, context: CheckerContext ->
        buildMapping(graph, context)
    }

    context(context: CheckerContext)
    fun resolveMappingOf(graph: ControlFlowGraph): ExpressionUniquenessStateMapping =
        cache.getValue(graph, context)

    private fun buildMapping(graph: ControlFlowGraph, context: CheckerContext): ExpressionUniquenessStateMapping {
        val outputs = context(context) { graph.resolveUniquenessStates() }
        val mapping = mutableMapOf<FirExpression, UniquenessState>()

        fun CFGNode<*>.inputState(): UniquenessState =
            previousCfgNodes
                .map { predecessor -> outputs[predecessor].asUniquenessState() }
                .reduceOrNull(UniquenessState::join)
                ?: EmptyUniquenessState

        fun register(expression: FirExpression, state: UniquenessState) {
            // Only register the first (earliest) state for a given expression.
            mapping.putIfAbsent(expression, state)
        }

        for (node in graph.nodes) {
            val inputState = node.inputState()
            val element = node.fir

            when (element) {
                is FirExpression -> {
                    register(element.unwrapExpression(), inputState)
                }
                else -> Unit
            }
        }

        return mapping
    }
}

private val FirSession.graphUniquenessStateMappingResolver: GraphUniquenessStateMappingResolver
    by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun ControlFlowGraph.resolveUniquenessStateMapping(): ExpressionUniquenessStateMapping =
    context.session.graphUniquenessStateMappingResolver.resolveMappingOf(this)
