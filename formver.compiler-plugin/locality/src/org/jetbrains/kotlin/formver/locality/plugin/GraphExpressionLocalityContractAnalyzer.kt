/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph

object GraphExpressionLocalityContractAnalyzer : GraphExpressionTailAnalyzer<LocalityContract>() {
    override val bottom = EmptyContract

    override fun LocalityContract.merge(other: LocalityContract): LocalityContract =
        union(other)

    override fun traverse(expression: FirExpression): LocalityContract =
        expression.resolveImmediateLocalityContract()
}

/**
 * Analyzes the locality contract of each expression in `this` control flow graph.
 *
 * The result is a map between [CFGNode]s and the [PathAwareLocalityContractFacts]s resulting after their execution.
 */
fun ControlFlowGraph.analyzeExpressionLocalityContracts(): Map<CFGNode<*>, PathAwareLocalityContractFacts> =
    GraphExpressionLocalityContractAnalyzer.analyzeFactsOf(this)
