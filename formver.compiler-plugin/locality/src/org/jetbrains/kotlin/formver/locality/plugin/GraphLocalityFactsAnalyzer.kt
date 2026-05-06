/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph

class GraphLocalityFactsAnalyzer(
    private val context: CheckerContext
) : GraphExpressionTailAnalyzer<Locality>() {
    override val bottom = Locality.Global

    override fun Locality.merge(other: Locality): Locality =
        union(other)

    override fun traverse(expression: FirExpression): Locality =
        if (expression is FirQualifiedAccessExpression) {
            with(context) {
                expression.resolveImmediateLocality()
            }
        } else {
            Locality.Global
        }

    fun analyzeLocalityOf(graph: ControlFlowGraph): Map<CFGNode<*>, PathAwareLocalityFacts> =
        analyzeFactsOf(graph)
}

/**
 * Analyzes the locality of each expression in `this` control flow graph.
 *
 * The result is a map between [CFGNode]s and the [PathAwareLocalityFacts]s resulting after their execution.
 */
context(context: CheckerContext)
fun ControlFlowGraph.analyzeLocalityFacts(): Map<CFGNode<*>, PathAwareLocalityFacts> =
    GraphLocalityFactsAnalyzer(context).analyzeLocalityOf(this)
