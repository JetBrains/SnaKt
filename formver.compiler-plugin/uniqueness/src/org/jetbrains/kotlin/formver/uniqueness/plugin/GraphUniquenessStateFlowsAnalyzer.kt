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
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.allReceiverExpressions
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNodeWithSubgraphs
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EqualityOperatorCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ExitSafeCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.QualifiedAccessNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TypeOperatorCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.formver.locality.plugin.Locality
import org.jetbrains.kotlin.formver.type.plugin.CallParametersTypeResolver

typealias UniquenessStateFlow = ControlFlowInfo<Unit, UniquenessState>

typealias PathAwareUniquenessStateFlow = PathAwareControlFlowInfo<Unit, UniquenessState>

class GraphUniquenessStatesAnalyzer(
    private val initialState: UniquenessState,
    private val context: CheckerContext,
    private val callParametersLocalityResolver: CallParametersTypeResolver<Locality>,
) : PathAwareControlFlowGraphVisitor<Unit, UniquenessState>() {
    override fun visitSubGraph(node: CFGNodeWithSubgraphs<*>, graph: ControlFlowGraph): Boolean {
        return false
    }

    override fun mergeInfo(
        a: UniquenessStateFlow,
        b: UniquenessStateFlow,
        node: CFGNode<*>
    ): UniquenessStateFlow =
        a.merge(b) { leftState, rightState ->
            leftState.join(rightState)
        }

    private fun UniquenessStateFlow.ensure(): UniquenessState =
        this[Unit] ?: initialState

    override fun visitNode(
        node: CFGNode<*>,
        data: PathAwareUniquenessStateFlow
    ): PathAwareUniquenessStateFlow {
        return data.transformValues { data -> data.put(Unit, data.ensure()) }
    }

    override fun visitQualifiedAccessNode(
        node: QualifiedAccessNode,
        data: PathAwareUniquenessStateFlow
    ): PathAwareUniquenessStateFlow {
        val qualifiedAccess = node.fir

        return data.transformValues { data ->
            with(context) {
                var newUniquenessState = data.ensure()
                val receiverAccessState = qualifiedAccess.pathReceiver?.resolveAccessState() ?: EmptyAccessState
                newUniquenessState = receiverAccessState.initialize(newUniquenessState)
                val selectorAccessState = qualifiedAccess.resolveAccessState()
                newUniquenessState = selectorAccessState.reserve(newUniquenessState)

                data.put(Unit, newUniquenessState)
            }
        }
    }


    override fun visitExitSafeCallNode(
        node: ExitSafeCallNode,
        data: PathAwareUniquenessStateFlow
    ): PathAwareUniquenessStateFlow {
        val safeCall = node.fir

        with(context) {
            return data.transformValues { data ->
                var newUniquenessState = data.ensure()
                val selector = safeCall.selector

                if (selector is FirExpression) {
                    val selectorAccessState = selector.resolveAccessState()
                    newUniquenessState = selectorAccessState.reserve(newUniquenessState)
                }

                newUniquenessState = safeCall.receiver.resolveAccessState().initialize(newUniquenessState)

                data.put(Unit, newUniquenessState)
            }
        }
    }

    private fun visitSyntheticCallNode(
        node: CFGNode<FirCall>,
        data: PathAwareUniquenessStateFlow
    ): PathAwareUniquenessStateFlow {
        val call = node.fir

        return data.transformValues { data ->
            with(context) {
                val uniquenessState = data.ensure()
                var newUniquenessState = uniquenessState

                for (argument in call.arguments) {
                    newUniquenessState = argument.resolveAccessState().initialize(newUniquenessState)
                }

                data.put(Unit, newUniquenessState)
            }
        }
    }

    override fun visitTypeOperatorCallNode(
        node: TypeOperatorCallNode,
        data: PathAwareUniquenessStateFlow
    ): PathAwareUniquenessStateFlow {
        val typeOperatorExpression = node.fir

        return when (typeOperatorExpression.operation) {
            FirOperation.AS, FirOperation.SAFE_AS -> data
            else -> visitSyntheticCallNode(node, data)
        }
    }

    override fun visitEqualityOperatorCallNode(
        node: EqualityOperatorCallNode,
        data: PathAwareUniquenessStateFlow
    ): PathAwareUniquenessStateFlow {
        return visitSyntheticCallNode(node, data)
    }

    override fun visitVariableDeclarationNode(
        node: VariableDeclarationNode,
        data: PathAwareUniquenessStateFlow
    ): PathAwareUniquenessStateFlow {
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
        data: PathAwareUniquenessStateFlow
    ): PathAwareUniquenessStateFlow {
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
        data: PathAwareUniquenessStateFlow
    ): PathAwareUniquenessStateFlow {
        val call = node.fir

        with(context) {
            return data.transformValues { data ->
                var newUniquenessState = data.ensure()

                for (receiver in call.allReceiverExpressions) {
                    newUniquenessState = receiver.resolveAccessState().move(newUniquenessState)
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
        data: PathAwareUniquenessStateFlow
    ): PathAwareUniquenessStateFlow {
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

fun PathAwareUniquenessStateFlow?.joinOverEdgeKinds(): UniquenessState =
    this?.values
        ?.map { it[Unit] ?: EmptyUniquenessState }
        ?.reduceOrNull(UniquenessState::join)
        ?: EmptyUniquenessState
