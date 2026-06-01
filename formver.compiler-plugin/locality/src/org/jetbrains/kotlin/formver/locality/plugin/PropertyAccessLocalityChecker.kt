/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.locality.plugin.LocalityErrors.INVALID_LOCALITY_CAPTURE

object PropertyAccessLocalityChecker : FirPropertyAccessExpressionChecker(MppCheckerKind.Common) {
    private fun FirDeclaration.declaresProperty(propertySymbol: FirPropertySymbol): Boolean {
        val graph = (this as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph
            ?: return false

        return graph.nodes.any { node ->
            (node.fir as? FirProperty)?.symbol == propertySymbol
        }
    }

    private fun FirDeclaration.declares(propertySymbol: FirBasedSymbol<*>): Boolean {
        return when (propertySymbol) {
            is FirReceiverParameterSymbol -> propertySymbol.containingDeclarationSymbol == symbol
            is FirValueParameterSymbol -> propertySymbol.containingDeclarationSymbol == symbol
            is FirPropertySymbol -> declaresProperty(propertySymbol)
            else -> false
        }
    }

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirPropertyAccessExpression) {
        val accessSymbol = expression.calleeReference.symbol ?: return

        if (expression.resolveLocality() == Locality.Global) return

        val outerDeclaration = context.findClosest<FirFunctionSymbol<*>>()?.fir ?: return

        if (outerDeclaration.declares(accessSymbol)) return

        reporter.reportOn(
            expression.source,
            INVALID_LOCALITY_CAPTURE,
            outerDeclaration.symbol,
        )
    }
}
