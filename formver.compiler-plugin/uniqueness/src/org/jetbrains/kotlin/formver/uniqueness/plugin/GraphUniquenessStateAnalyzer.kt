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
import org.jetbrains.kotlin.formver.locality.plugin.Locality
import org.jetbrains.kotlin.formver.type.plugin.CallParametersTypeResolver

class GraphUniquenessStateAnalyzer(
    private val initialState: UniquenessState,
    private val context: CheckerContext,
    private val callParametersLocalityResolver: CallParametersTypeResolver<Locality>,
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
        this[Unit] ?: initialState

    override fun visitNode(
        node: CFGNode<*>,
        data: PathAwareControlFlowInfo<Unit, UniquenessState>
    ): PathAwareControlFlowInfo<Unit, UniquenessState> {
        return data.transformValues { data -> data.put(Unit, data.read()) }
    }

    override fun visitVariableDeclarationNode(
        node: VariableDeclarationNode,
        data: PathAwareControlFlowInfo<Unit, UniquenessState>
    ): PathAwareControlFlowInfo<Unit, UniquenessState> {
        val declaration = node.fir
        val initializer = declaration.initializer

        with(context) {
            val declarationSymbol = declaration.symbol
            val declarationAccessState = EmptyAccessState.associate(
                declarationSymbol,
                AccessState(true)
            )

            return data.transformValues { data ->
                var uniquenessState = data.read()

                uniquenessState = declarationAccessState.initialize(uniquenessState)

                if (initializer != null) {
                    val initializerAccessState = initializer.resolveAccessState()
                    uniquenessState = initializerAccessState.move(uniquenessState)
                }

                data.put(Unit, uniquenessState)
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
                uniquenessState = rightAccessState.move(uniquenessState)

                if (leftAccessState.isSingleton()) {
                    uniquenessState = leftAccessState.initialize(uniquenessState)
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
                moveState = argument.resolveAccessState().move(moveState)
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
            for ((argument, requiredLocality) in callParametersLocalityResolver.resolveParameterTypesOf(call)) {
                if (requiredLocality == null) continue

                initializationState = argument.resolveAccessState().initialize(initializationState)
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
