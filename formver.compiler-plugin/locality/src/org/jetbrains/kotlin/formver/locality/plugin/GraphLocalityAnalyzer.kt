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
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.BlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.BooleanOperatorExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNodeWithSubgraphs
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EdgeLabel
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ElvisExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.JumpNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TryExpressionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TryMainBlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TypeOperatorCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenBranchResultExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenExitNode

/**
 * Contains locality information for each `FirExpression` in the control flow, normalized through [unwrapExpression].
 */
typealias LocalityFacts = PersistentMap<FirExpression, Locality>

/**
 * Contains locality information for every type of execution flow.
 */
typealias PathAwareLocalityFacts = PersistentMap<EdgeLabel, LocalityFacts>

/**
 * Merges the locality information for every type of execution flow.
 */
fun PathAwareLocalityFacts.collapse(): LocalityFacts =
    values.fold(persistentMapOf()) { result, info ->
        result.merge(info, Locality::union)
    }

/**
 * Stub expression for tracking the current tail locality.
 */
private val TailExpression: FirExpression = buildExpressionStub()

/**
 * Returns true if `this` node propagates the locality to the successor.
 */
private fun CFGNode<*>.inheritsTailFact(): Boolean {
    return this is BlockExitNode ||
            this is WhenExitNode ||
            this is WhenBranchResultExitNode ||
            this is BooleanOperatorExitNode ||
            this is JumpNode ||
            this is ElvisExitNode ||
            this is TryExpressionExitNode ||
            this is TryMainBlockExitNode ||
            this is TypeOperatorCallNode && (fir.operation == FirOperation.AS || fir.operation == FirOperation.SAFE_AS)
}

class GraphLocalityAnalyzer(
    private val context: CheckerContext
) : PathAwareControlFlowGraphVisitor<FirExpression, Locality>() {
    override fun mergeInfo(
        a: LocalityFacts,
        b: LocalityFacts,
        node: CFGNode<*>
    ): LocalityFacts =
        a.merge(b, Locality::union)

    override fun visitSubGraph(node: CFGNodeWithSubgraphs<*>, graph: ControlFlowGraph): Boolean {
        return false
    }

    override fun visitNode(
        node: CFGNode<*>,
        data: PathAwareLocalityFacts
    ): PathAwareLocalityFacts {
        val inheritsTailFact = node.inheritsTailFact()

        return data.transformValues { facts ->
            when (val element = node.fir) {
                is FirExpression -> {
                    val expression = element.unwrapExpression()

                    when {
                        inheritsTailFact ->
                            facts.put(expression, facts[TailExpression] ?: Locality.Global)
                        else -> {
                            val expressionLocality =
                                if (expression is FirQualifiedAccessExpression) {
                                    with(context) {
                                        expression.resolveImmediateLocality()
                                    }
                                } else {
                                    Locality.Global
                                }

                            facts.put(expression, expressionLocality)
                                .put(TailExpression, expressionLocality)
                        }
                    }
                }

                else ->
                    when {
                        inheritsTailFact -> facts
                        else -> facts.remove(TailExpression)
                    }
            }
        }
    }

    fun analyzeLocalityOf(graph: ControlFlowGraph): Map<CFGNode<*>, PathAwareLocalityFacts> =
        graph.traverseToFixedPoint(this)
}

/**
 * Analyzes the locality of each expression in `this` control flow graph.
 *
 * The result is a map between [CFGNode]s and the [PathAwareLocalityFacts]s resulting after their execution.
 */
context(context: CheckerContext)
fun ControlFlowGraph.analyzeLocality(): Map<CFGNode<*>, PathAwareLocalityFacts> =
    GraphLocalityAnalyzer(context).analyzeLocalityOf(this)
