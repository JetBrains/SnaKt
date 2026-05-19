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
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.formver.locality.plugin.Locality
import org.jetbrains.kotlin.formver.type.plugin.CallParametersTypeResolver

class GraphUniquenessStateResolver(
    private val initial: UniquenessTrie,
    private val context: CheckerContext,
    private val callArgumentsLocalityResolver: CallParametersTypeResolver<Locality>
) : PathAwareControlFlowGraphVisitor<Unit, UniquenessTrie>() {
    override fun mergeInfo(
        a: ControlFlowInfo<Unit, UniquenessTrie>,
        b: ControlFlowInfo<Unit, UniquenessTrie>,
        node: CFGNode<*>
    ): ControlFlowInfo<Unit, UniquenessTrie> {
        return a.merge(b) { leftTrie, rightTrie ->
            leftTrie.join(rightTrie)
        }
    }

    private fun ControlFlowInfo<Unit, UniquenessTrie>.read(): UniquenessTrie =
        this[Unit] ?: initial

    override fun visitVariableAssignmentNode(
        node: VariableAssignmentNode,
        data: PathAwareControlFlowInfo<Unit, UniquenessTrie>
    ): PathAwareControlFlowInfo<Unit, UniquenessTrie> {
        val assignment = node.fir
        val moveState = assignment.rValue.resolveMoveUniqueness()

        return data.transformValues { data ->
            data.put(Unit, data.read().join(moveState))
        }
    }

    override fun visitFunctionCallExitNode(
        node: FunctionCallExitNode,
        data: PathAwareControlFlowInfo<Unit, UniquenessTrie>
    ): PathAwareControlFlowInfo<Unit, UniquenessTrie> {
        val call = node.fir
        var moveState = UniquenessRoot

        with (context) {
            for ((argument, requiredLocality) in callArgumentsLocalityResolver.resolveParameterTypesOf(call)) {
                if (requiredLocality != null) {
                    moveState = moveState.join(argument.resolveMoveUniqueness())
                }
            }
        }

        return data.transformValues { data ->
            data.put(Unit, data.read().join(moveState))
        }
    }
}
