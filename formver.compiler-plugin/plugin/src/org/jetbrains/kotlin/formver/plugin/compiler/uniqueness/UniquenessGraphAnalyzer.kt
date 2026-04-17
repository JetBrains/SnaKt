/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.uniqueness

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.traverseToFixedPoint
import org.jetbrains.kotlin.fir.analysis.cfa.util.transformValues
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.QualifiedAccessNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.toQualifiedAccess
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.join
import org.jetbrains.kotlin.formver.plugin.compiler.locality.Locality
import org.jetbrains.kotlin.formver.plugin.compiler.locality.extractRequiredLocality

typealias UniquenessPathAwareFlow = Map<CFGNode<*>, PathAwareControlFlowInfo<Unit, UniquenessState>>

/**
 * Uniqueness analysis over CFG nodes using FIR CFA fixed-point traversal.
 */
class UniquenessGraphAnalyzer(
    private val initial: UniquenessState,
    private val context: CheckerContext,
) : PathAwareControlFlowGraphVisitor<Unit, UniquenessState>() {
    private val bottom = UniquenessState(Uniqueness.Unique)

    private val evaluator = object : ControlFlowGraphVisitor<UniquenessState, UniquenessState>() {
        override fun visitNode(
            node: CFGNode<*>,
            data: UniquenessState
        ): UniquenessState {
            return data
        }

        override fun visitVariableDeclarationNode(
            node: VariableDeclarationNode,
            data: UniquenessState
        ): UniquenessState {
            return node.fir.toQualifiedAccess().extractPostDefinitionState(data)
        }

        override fun visitVariableAssignmentNode(
            node: VariableAssignmentNode,
            data: UniquenessState
        ): UniquenessState {
            return node.fir.lValue.extractPostDefinitionState(data)
        }

        @OptIn(SymbolInternals::class)
        override fun visitFunctionCallEnterNode(
            node: FunctionCallEnterNode,
            data: UniquenessState
        ): UniquenessState {
            val function = node.fir.calleeReference.symbol?.fir as? FirFunction ?: return data
            var result = data

            for ((argument, parameter) in node.fir.arguments.zip(function.valueParameters)) {
                val parameterLocality = with(context) {
                    parameter.extractRequiredLocality()
                }

                if (parameterLocality is Locality.Local) {
                    result = argument.extractPostDefinitionState(result)
                }
            }

            return result
        }

        override fun visitQualifiedAccessNode(node: QualifiedAccessNode, data: UniquenessState): UniquenessState {
            return node.fir.extractPostUseState(data)
        }
    }

    override fun mergeInfo(
        a: PersistentMap<Unit, UniquenessState>,
        b: PersistentMap<Unit, UniquenessState>,
        node: CFGNode<*>
    ): PersistentMap<Unit, UniquenessState> {
        val left = a[Unit] as? UniquenessState
        val right = b[Unit] as? UniquenessState
        val merged = when {
            left == null -> right
            right == null -> left
            else -> left.join(right)
        }

        return if (merged == null) {
            persistentMapOf()
        } else {
            persistentMapOf(Unit to merged)
        }
    }

    override fun visitNode(
        node: CFGNode<*>,
        data: PathAwareControlFlowInfo<Unit, UniquenessState>
    ): PathAwareControlFlowInfo<Unit, UniquenessState> {
        return data.transformValues { info ->
            val default = if (node == node.owner.enterNode) initial else bottom
            val preState = (info[Unit] as? UniquenessState) ?: default
            persistentMapOf(Unit to node.accept(evaluator, preState))
        }
    }
}

context(context: CheckerContext)
fun ControlFlowGraph.analyzeUniqueness(
    initial: UniquenessState
): UniquenessPathAwareFlow {
    return traverseToFixedPoint(UniquenessGraphAnalyzer(initial, context))
}
