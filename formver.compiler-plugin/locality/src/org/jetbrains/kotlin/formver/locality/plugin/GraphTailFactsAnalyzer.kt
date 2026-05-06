/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.analysis.cfa.util.ControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.merge
import org.jetbrains.kotlin.fir.analysis.cfa.util.transformValues
import org.jetbrains.kotlin.fir.analysis.cfa.util.traverseToFixedPoint
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.BlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.BooleanOperatorExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNodeWithSubgraphs
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ElvisExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.JumpNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TryExpressionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TryMainBlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TypeOperatorCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenBranchResultExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenExitNode

/**
 * Stub expression for tracking the current tail fact.
 */
private val TailExpression: FirExpression = buildExpressionStub()

/**
 * Returns true if `this` node propagates the current tail fact to the successor.
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

abstract class GraphTailFactsAnalyzer<F : Any> : PathAwareControlFlowGraphVisitor<FirExpression, F>() {
    protected abstract val bottom: F

    protected abstract fun F.merge(other: F): F

    protected abstract fun traverse(expression: FirExpression): F

    override fun mergeInfo(
        a: ControlFlowFacts<FirExpression, F>,
        b: ControlFlowFacts<FirExpression, F>,
        node: CFGNode<*>
    ): ControlFlowInfo<FirExpression, F> =
        a.merge(b) { a, b ->
            a.merge(b)
        }

    override fun visitSubGraph(node: CFGNodeWithSubgraphs<*>, graph: ControlFlowGraph): Boolean {
        return false
    }

    override fun visitNode(
        node: CFGNode<*>,
        data: PathAwareControlFlowInfo<FirExpression, F>
    ): PathAwareControlFlowInfo<FirExpression, F> {
        val inheritsTailFact = node.inheritsTailFact()

        return data.transformValues { facts ->
            when (val element = node.fir) {
                is FirExpression -> {
                    val expression = element.unwrapExpression()

                    when {
                        inheritsTailFact ->
                            facts.put(expression, facts[TailExpression] ?: bottom)
                        else -> {
                            val expressionFact = traverse(expression)

                            facts.put(expression, expressionFact)
                                .put(TailExpression, expressionFact)
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

    fun analyzeFactsOf(graph: ControlFlowGraph): Map<CFGNode<*>, PathAwareControlFlowInfo<FirExpression, F>> =
        graph.traverseToFixedPoint(this)
}
