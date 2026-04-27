/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.analysis.cfa.util.merge
import org.jetbrains.kotlin.fir.analysis.cfa.util.transformValues
import org.jetbrains.kotlin.fir.analysis.cfa.util.traverseToFixedPoint
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EdgeLabel
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ExitSafeCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TailrecExitNodeMarker
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TryExpressionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TryMainBlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TypeOperatorCallNode

/**
 * Contains locality information for each `FirExpression` in the control flow, normalized through [unwrapExpression].
 */
typealias LocalityInfo = PersistentMap<FirExpression, Locality>

/**
 * Contains locality information for every type of execution flow.
 */
typealias LocalityFlow = PersistentMap<EdgeLabel, LocalityInfo>

/**
 * Merges the locality information for every type of execution flow.
 */
fun LocalityFlow.collapse(): LocalityInfo =
    values.fold(persistentMapOf()) { result, info ->
        result.merge(info, Locality::union)
    }

/**
 * Stub expression for tracking the current tail locality.
 */
private val TailExpression: FirExpression = buildExpressionStub()

private fun CFGNode<*>.inheritsTailFact(): Boolean {
    return (this is TailrecExitNodeMarker && this !is ExitSafeCallNode) ||
            this is TryExpressionExitNode ||
            this is TryMainBlockExitNode ||
            (this is TypeOperatorCallNode && fir.operation == FirOperation.AS)
}

class GraphLocalityAnalyzer(
    private val context: CheckerContext
) : PathAwareControlFlowGraphVisitor<FirExpression, Locality>() {
    override fun mergeInfo(
        a: LocalityInfo,
        b: LocalityInfo,
        node: CFGNode<*>
    ): LocalityInfo =
        a.merge(b, Locality::union)

    override fun visitNode(
        node: CFGNode<*>,
        data: LocalityFlow
    ): LocalityFlow {
        val inheritsTailFact = node.inheritsTailFact()

        return data.transformValues { info ->
            when (val element = node.fir) {
                is FirExpression -> {
                    val expression = element.unwrapExpression()

                    when {
                        inheritsTailFact ->
                            info.put(expression, info[TailExpression] ?: Global)
                        else -> {
                            val expressionLocality = with(context) {
                                expression.resolveComponentLocality()
                            } ?: Global

                            info.put(expression, expressionLocality)
                                .put(TailExpression, expressionLocality)
                        }
                    }
                }

                else ->
                    when {
                        inheritsTailFact -> info
                        else -> info.remove(TailExpression)
                    }
            }
        }
    }

    fun analyzeLocalityOf(graph: ControlFlowGraph): Map<CFGNode<*>, LocalityFlow> =
        graph.traverseToFixedPoint(this)
}

/**
 * Analyzes the locality of each expression in `this` control flow graph.
 *
 * The result is a map between [CFGNode]s and the [LocalityFlow]s resulting after their execution.
 */
context(context: CheckerContext)
fun ControlFlowGraph.analyzeLocality(): Map<CFGNode<*>, LocalityFlow> =
    traverseToFixedPoint(GraphLocalityAnalyzer(context))
