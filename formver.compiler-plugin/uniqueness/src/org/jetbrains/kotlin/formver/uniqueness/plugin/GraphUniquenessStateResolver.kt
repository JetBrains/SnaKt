/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.traverseToFixedPoint
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.formver.locality.plugin.CallParametersLocalityResolver

typealias GraphUniquenessStates = Map<CFGNode<*>, PathAwareControlFlowInfo<Unit, UniquenessState>>

class GraphUniquenessStateResolver(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> GraphUniquenessStateResolver(session) }
        }
    }

    private val cache = session.firCachesFactory.createCache { graph: ControlFlowGraph, context: CheckerContext ->
        analyzeUniquenessStatesOf(graph, context)
    }

    fun resolveUniquenessStatesOf(graph: ControlFlowGraph, context: CheckerContext): GraphUniquenessStates =
        cache.getValue(graph, context)

    private fun analyzeUniquenessStatesOf(graph: ControlFlowGraph, context: CheckerContext): GraphUniquenessStates {
        val declaration = graph.declaration
        var initial = EmptyUniquenessState

        if (declaration is FirFunction) {
            for (valueParameter in declaration.valueParameters) {
                val valueParameterSymbol = valueParameter.symbol

                initial = context(context) {
                    initial.associate(
                        valueParameterSymbol,
                        UniquenessState(valueParameterSymbol.resolveUniqueness())
                    )
                }
            }
        }

        val analyzer = GraphUniquenessStateAnalyzer(
            initial,
            context,
            CallParametersLocalityResolver
        )

        return graph.traverseToFixedPoint(analyzer)
    }
}

private val FirSession.graphUniquenessStateResolver: GraphUniquenessStateResolver
        by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun ControlFlowGraph.resolveUniquenessStates(): GraphUniquenessStates =
    context.session.graphUniquenessStateResolver.resolveUniquenessStatesOf(this, context)
