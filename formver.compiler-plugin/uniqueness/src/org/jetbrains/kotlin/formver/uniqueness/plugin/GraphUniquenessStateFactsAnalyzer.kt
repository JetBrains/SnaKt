/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.analysis.cfa.util.ControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.merge
import org.jetbrains.kotlin.fir.analysis.cfa.util.transformValues
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNodeWithSubgraphs
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EqualityOperatorCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.QualifiedAccessNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TypeOperatorCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.formver.locality.plugin.Locality
import org.jetbrains.kotlin.formver.type.plugin.CallParametersTypeResolver

typealias UniquenessStateFacts = ControlFlowInfo<Unit, UniquenessState>

typealias PathAwareUniquenessStateFacts = PathAwareControlFlowInfo<Unit, UniquenessState>

class GraphUniquenessStateFactsAnalyzer(
    private val initialState: UniquenessState,
    private val context: CheckerContext,
    private val callParametersLocalityResolver: CallParametersTypeResolver<Locality>,
) : PathAwareControlFlowGraphVisitor<Unit, UniquenessState>() {
    override fun visitSubGraph(node: CFGNodeWithSubgraphs<*>, graph: ControlFlowGraph): Boolean {
        return false
    }

    override fun mergeInfo(
        a: UniquenessStateFacts,
        b: UniquenessStateFacts,
        node: CFGNode<*>
    ): UniquenessStateFacts =
        a.merge(b) { leftState, rightState ->
            leftState.join(rightState)
        }

    private fun UniquenessStateFacts.ensure(): UniquenessState =
        this[Unit] ?: initialState

    override fun visitNode(
        node: CFGNode<*>,
        data: PathAwareUniquenessStateFacts
    ): PathAwareUniquenessStateFacts {
        return data.transformValues { data -> data.put(Unit, data.ensure()) }
    }

    override fun visitQualifiedAccessNode(
        node: QualifiedAccessNode,
        data: PathAwareControlFlowInfo<Unit, UniquenessState>
    ): PathAwareControlFlowInfo<Unit, UniquenessState> {
        return data.transformValues { data ->
            val qualifiedAccess = node.fir

            with(context) {
                val uniquenessState = data.ensure()
                var newUniquenessState = uniquenessState
                val receiverAccessState = qualifiedAccess.pathReceiver?.resolveAccessState() ?: EmptyAccessState
                newUniquenessState = receiverAccessState.initialize(newUniquenessState)

                val selectorAccessState = qualifiedAccess.resolveAccessState()
                newUniquenessState = selectorAccessState.access(newUniquenessState)

                data.put(Unit, newUniquenessState)
            }
        }
    }

    override fun visitTypeOperatorCallNode(
        node: TypeOperatorCallNode,
        data: PathAwareControlFlowInfo<Unit, UniquenessState>
    ): PathAwareControlFlowInfo<Unit, UniquenessState> {
        val typeOperatorExpression = node.fir
        val arguments = typeOperatorExpression.arguments

        return when (typeOperatorExpression.operation) {
            FirOperation.AS, FirOperation.SAFE_AS ->
                data
            else ->
                data.transformValues { data ->
                    with(context) {
                        val uniquenessState = data.ensure()
                        var newUniquenessState = uniquenessState

                        for (argument in arguments) {
                            newUniquenessState = argument.resolveAccessState().initialize(newUniquenessState)
                        }

                        data.put(Unit, newUniquenessState)
                    }
                }
        }
    }

    override fun visitEqualityOperatorCallNode(
        node: EqualityOperatorCallNode,
        data: PathAwareControlFlowInfo<Unit, UniquenessState>
    ): PathAwareControlFlowInfo<Unit, UniquenessState> {
        val comparisonExpression = node.fir
        val arguments = comparisonExpression.arguments

        return data.transformValues { data ->
            with(context) {
                val uniquenessState = data.ensure()
                var newUniquenessState = uniquenessState

                for (argument in arguments) {
                    newUniquenessState = argument.resolveAccessState().initialize(newUniquenessState)
                }

                data.put(Unit, newUniquenessState)
            }
        }
    }

    override fun visitVariableDeclarationNode(
        node: VariableDeclarationNode,
        data: PathAwareUniquenessStateFacts
    ): PathAwareUniquenessStateFacts {
        val declaration = node.fir
        val initializer = declaration.initializer
        val leftSymbol = declaration.symbol
        val leftAccessState = EmptyAccessState.associate(
            leftSymbol,
            AccessState(Access.Terminal)
        )

        with(context) {
            val rightAccessState = initializer?.resolveAccessState() ?: EmptyAccessState

            return data.transformValues { data ->
                val uniquenessState = data.ensure()
                var newUniquenessState = uniquenessState

                if (initializer != null) {
                    val rightAccessState = initializer.resolveAccessState()
                    val rightUniquenessState = rightAccessState.joinUniquenessStateOverTerminals(uniquenessState)
                    newUniquenessState = newUniquenessState.insert(listOf(leftSymbol), rightUniquenessState)
                }

                newUniquenessState = leftAccessState.initialize(newUniquenessState)
                newUniquenessState = rightAccessState.move(newUniquenessState)
                data.put(Unit, newUniquenessState)
            }
        }
    }

    override fun visitVariableAssignmentNode(
        node: VariableAssignmentNode,
        data: PathAwareUniquenessStateFacts
    ): PathAwareUniquenessStateFacts {
        val assignment = node.fir
        val leftValue = assignment.lValue
        val rightValue = assignment.rValue

        with(context) {
            val leftAccessState = leftValue.resolveAccessState()

            return data.transformValues { data ->
                var newUniquenessState = data.ensure()
                val leftAccessPaths = leftAccessState.enumerateTerminalPaths()
                val rightAccessState = rightValue.resolveAccessState()

                if (leftAccessPaths.count() == 1) {
                    val leftPath = leftAccessPaths.first()
                    val rightUniquenessState = rightAccessState.joinUniquenessStateOverTerminals(newUniquenessState)
                    newUniquenessState = newUniquenessState.insert(leftPath, rightUniquenessState)
                }

                when (leftValue) {
                    is FirQualifiedAccessExpression -> {
                        val receiverAccessState = leftValue.pathReceiver?.resolveAccessState() ?: EmptyAccessState
                        newUniquenessState = receiverAccessState.initialize(newUniquenessState)
                    }
                }

                newUniquenessState = leftAccessState.initialize(newUniquenessState)
                newUniquenessState = rightAccessState.move(newUniquenessState)

                data.put(Unit, newUniquenessState)
            }
        }
    }

    override fun visitFunctionCallEnterNode(
        node: FunctionCallEnterNode,
        data: PathAwareUniquenessStateFacts
    ): PathAwareUniquenessStateFacts {
        val call = node.fir

        with(context) {

            return data.transformValues { data ->
                var newUniquenessState = data.ensure()
                val callReceiver = call.pathReceiver

                if (callReceiver != null) {
                    newUniquenessState = callReceiver.resolveAccessState().move(newUniquenessState)
                }

                for (argument in call.arguments) {
                    newUniquenessState = argument.resolveAccessState().move(newUniquenessState)
                }

                data.put(Unit, newUniquenessState)
            }
        }
    }

    override fun visitFunctionCallExitNode(
        node: FunctionCallExitNode,
        data: PathAwareUniquenessStateFacts
    ): PathAwareUniquenessStateFacts {
        val call = node.fir

        with(context) {
            return data.transformValues { data ->
                var newUniquenessState = data.ensure()

                for ((argument, requiredLocality) in callParametersLocalityResolver.resolveParameterTypesOf(call)) {
                    if (requiredLocality == null) continue

                    newUniquenessState = argument.resolveAccessState().initialize(newUniquenessState)
                }

                data.put(Unit, newUniquenessState)
            }
        }
    }
}

fun PathAwareUniquenessStateFacts?.joinOverEdgeKinds(): UniquenessState =
    this?.values
        ?.map { it[Unit] ?: EmptyUniquenessState }
        ?.reduceOrNull(UniquenessState::join)
        ?: EmptyUniquenessState
