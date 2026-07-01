package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.analysis.cfa.util.previousCfgNodes
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.BlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.JumpNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ThrowExceptionNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.locality.plugin.Locality
import org.jetbrains.kotlin.formver.locality.plugin.LocalityAttribute
import org.jetbrains.kotlin.formver.locality.plugin.resolveLocality
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessErrors.CAPTURED_UNIQUENESS_INCONSISTENCY

context(context: CheckerContext)
val FirBasedSymbol<*>.locality: Locality
    get() = when (this) {
        is FirVariableSymbol<*> -> resolveLocality()
        is FirReceiverParameterSymbol -> resolveLocality()
        else -> null
    }

object FunctionInputUniquenessConsistencyChecker
    : FirFunctionChecker( MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        val graph = declaration.controlFlowGraphReference?.controlFlowGraph
            ?: return
        // TODO: Resolve the input uniqueness state flows (the output works but it doesn't make much sense)
        val uniquenessFlows = graph.resolveUniquenessStateFlows()
        fun CFGNode<*>.isExitJump(): Boolean =
            when (this) {
                is ThrowExceptionNode -> true
                is JumpNode -> {
                    val jumpTarget = fir.target

                    jumpTarget is FirFunctionTarget && jumpTarget.labeledElement == declaration
                }
                is BlockExitNode -> {
                    !isDead && fir == declaration.body
                }
                else -> false
            }

        for (node in graph.nodes) {
            if (!node.isExitJump()) continue

            val inputUniquenessState = uniquenessFlows[node].joinOverEdgeKinds()
            val topLevelUniquenessStates = inputUniquenessState.children

            for ((symbol, uniquenessState) in topLevelUniquenessStates) {
                if (symbol.locality == LocalityAttribute) {
                    val inconsistentPaths = uniquenessState.enumerateInconsistentPaths()

                    for (inconsistentPath in inconsistentPaths) {
                        reporter.reportOn(
                            node.fir.source ?: declaration.source,
                            CAPTURED_UNIQUENESS_INCONSISTENCY,
                            inconsistentPath
                        )
                    }
                }
            }
        }
    }
}
