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
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.formver.type.plugin.CallParametersTypeResolver

class GraphUniquenessStateAnalyzer(
    private val initial: UniquenessState,
    private val context: CheckerContext,
    private val callParametersUniquenessResolver: CallParametersTypeResolver<Uniqueness>
) : PathAwareControlFlowGraphVisitor<Unit, UniquenessState>() {
    override fun mergeInfo(
        a: ControlFlowInfo<Unit, UniquenessState>,
        b: ControlFlowInfo<Unit, UniquenessState>,
        node: CFGNode<*>
    ): ControlFlowInfo<Unit, UniquenessState> =
        a.merge(b) { leftState, rightState ->
            leftState.join(rightState)
        }

    private fun ControlFlowInfo<Unit, UniquenessState>.read(): UniquenessState =
        this[Unit] ?: initial

    override fun visitVariableDeclarationNode(
        node: VariableDeclarationNode,
        data: PathAwareControlFlowInfo<Unit, UniquenessState>
    ): PathAwareControlFlowInfo<Unit, UniquenessState> {
        val declaration = node.fir

        with (context) {
            val declarationSymbol = declaration.symbol
            val declarationUniquenessState = EmptyUniquenessState.associate(
                declarationSymbol,
                UniquenessState(declarationSymbol.resolveUniqueness())
            )

            return data.transformValues { data ->
                val uniquenessState = data.read()

                data.put(Unit, uniquenessState.join(declarationUniquenessState))
            }
        }
    }

    override fun visitVariableAssignmentNode(
        node: VariableAssignmentNode,
        data: PathAwareControlFlowInfo<Unit, UniquenessState>
    ): PathAwareControlFlowInfo<Unit, UniquenessState> {
        val assignment = node.fir

        with(context) {
            val leftAccessState = assignment.lValue.resolveAccessState()
            val rightAccessState = assignment.rValue.resolveAccessState()

            return data.transformValues { data ->
                var uniquenessState = data.read()
                uniquenessState = rightAccessState.maskMove(uniquenessState)

                if (leftAccessState.isChain()) {
                    uniquenessState = leftAccessState.maskInitialization(uniquenessState)
                }

                data.put(Unit, uniquenessState)
            }
        }
    }

    override fun visitFunctionCallEnterNode(
        node: FunctionCallEnterNode,
        data: PathAwareControlFlowInfo<Unit, UniquenessState>
    ): PathAwareControlFlowInfo<Unit, UniquenessState> {
        val call = node.fir

        var moveState = EmptyUniquenessState

        with(context) {
            for (argument in call.arguments) {
                moveState = argument.resolveAccessState().maskMove(moveState)
            }

            return data.transformValues { data ->
                data.put(Unit, data.read().join(moveState))
            }
        }
    }

    override fun visitFunctionCallExitNode(
        node: FunctionCallExitNode,
        data: PathAwareControlFlowInfo<Unit, UniquenessState>
    ): PathAwareControlFlowInfo<Unit, UniquenessState> {
        val call = node.fir
        var initializationState = EmptyUniquenessState

        with(context) {
            for ((argument, requiredUniqueness) in callParametersUniquenessResolver.resolveParameterTypesOf(call)) {
                if (requiredUniqueness != Uniqueness.Shared) continue

                initializationState = argument.resolveAccessState().maskInitialization(initializationState)
            }

            return data.transformValues { data ->
                data.put(Unit, data.read().join(initializationState) { a, b -> a.meet(b) })
            }
        }
    }
}

fun PathAwareControlFlowInfo<Unit, UniquenessState>?.asUniquenessState(): UniquenessState =
    this?.values
        ?.map { it[Unit] ?: EmptyUniquenessState }
        ?.reduceOrNull(UniquenessState::join)
        ?: EmptyUniquenessState
