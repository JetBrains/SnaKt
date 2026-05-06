/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.isResolved
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.type

class GraphLocalityContractFactsAnalyzer(
    private val context: CheckerContext
) : GraphTailFactsAnalyzer<LocalityContract>() {
    override val bottom = LocalityContract.Undefined

    override fun LocalityContract.merge(other: LocalityContract): LocalityContract =
        join(other)

    override fun traverse(expression: FirExpression): LocalityContract {
        if (!expression.isResolved) return LocalityContract.Undefined

        val type = expression.resolvedType

        if (type is ConeClassLikeType && type.isSomeFunctionType(context.session)) {
            val inputProjections = type.typeArguments.dropLast(1)

            return LocalityContract.FunctionContract(
                inputProjections.map { argumentProjection ->
                    argumentProjection.type?.resolveLocalityRequirement() ?: LocalityRequirement.RequireGlobal
                }
            )
        }

        return LocalityContract.Undefined
    }
}

/**
 * Analyzes the locality-contracts of each expression in `this` control flow graph.
 *
 * The result is a map between [CFGNode]s and the [PathAwareLocalityContractFacts]s resulting after their execution.
 */
context(context: CheckerContext)
fun ControlFlowGraph.analyzeLocalityContractFacts(): Map<CFGNode<*>, PathAwareLocalityContractFacts> =
    GraphLocalityContractFactsAnalyzer(context).analyzeFactsOf(this)
