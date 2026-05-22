/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.cfa.util.previousCfgNodes
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.unwrapAndFlattenArgument
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.JumpNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ThrowExceptionNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode

object GraphUniquenessChecker : FirControlFlowChecker(MppCheckerKind.Common) {
    context(reporter: DiagnosticReporter, context: CheckerContext)
    override fun analyze(graph: ControlFlowGraph) {
        val uniquenessStates = graph.resolveUniquenessStates()

        fun CFGNode<*>.inputState(): UniquenessState =
            previousCfgNodes
                .map { predecessor -> uniquenessStates[predecessor].asUniquenessState() }
                .reduceOrNull(UniquenessState::join)
                ?: EmptyUniquenessState

        for (node in graph.nodes) {
            val inputState = node.inputState()

            when (node) {
                is VariableDeclarationNode -> {
                    val initializer = node.fir.initializer ?: continue
                    checkUniqueness(
                        "Declaration",
                        node.fir.symbol.resolveUniqueness(),
                        initializer.resolveUniqueness(inputState),
                        initializer
                    )
                }

                is VariableAssignmentNode -> {
                    val assignment = node.fir
                    checkUniqueness(
                        "Assignment",
                        assignment.lValue.resolveRequiredUniqueness(),
                        assignment.rValue.resolveUniqueness(inputState),
                        assignment.rValue
                    )
                }

                is FunctionCallEnterNode -> {
                    var argumentState = inputState

                    for ((argument, requiredUniqueness) in CallParametersUniquenessResolver.resolveParameterTypesOf(node.fir)) {
                        for (effectiveArgument in argument.unwrapAndFlattenArgument(flattenArrays = false)) {
                            checkUniqueness(
                                "Argument",
                                requiredUniqueness,
                                effectiveArgument.resolveUniqueness(argumentState),
                                effectiveArgument
                            )
                        }

                        argumentState = argument.resolveAccessState().maskMove(argumentState)
                    }
                }

                is JumpNode -> {
                    val returnExpression = node.fir as? FirReturnExpression ?: continue
                    val result = returnExpression.result

                    checkUniqueness(
                        "Return",
                        Uniqueness.Shared,
                        result.resolveUniqueness(inputState),
                        result
                    )
                }

                is ThrowExceptionNode -> {
                    val exception = node.fir.exception
                    checkUniqueness(
                        "Throw",
                        Uniqueness.Shared,
                        exception.resolveUniqueness(inputState),
                        exception
                    )
                }

                else -> Unit
            }
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkUniqueness(
        diagnosticKind: String,
        requiredUniqueness: Uniqueness,
        actualUniqueness: Uniqueness,
        expression: FirExpression
    ) {
        if (requiredUniqueness.accepts(actualUniqueness)) return

        reporter.reportOn(
            expression.source,
            UniquenessErrors.UNIQUENESS_MISMATCH,
            diagnosticKind,
            requiredUniqueness,
            actualUniqueness
        )
    }
}

context(context: CheckerContext)
private fun FirExpression.resolveRequiredUniqueness(): Uniqueness =
    resolveAccessState().resolveUniqueness()
