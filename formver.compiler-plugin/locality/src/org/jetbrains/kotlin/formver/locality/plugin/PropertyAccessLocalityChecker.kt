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
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol

object PropertyAccessLocalityChecker : FirPropertyAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext)
    private fun FirFunctionSymbol<*>.declares(propertySymbol: FirBasedSymbol<*>): Boolean =
        when (propertySymbol) {
            is FirReceiverParameterSymbol ->
                propertySymbol.containingDeclarationSymbol == this
            is FirValueParameterSymbol ->
                propertySymbol.containingDeclarationSymbol == this
            is FirLocalPropertySymbol -> {
                val graph = resolvedControlFlowGraphReference?.controlFlowGraph ?: return false

                propertySymbol in graph.resolveLocalPropertySymbols()
            }
            else -> false
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirPropertyAccessExpression) {
        val accessSymbol = expression.calleeReference.symbol ?: return

        if (expression.resolveLocality() == Locality.Global) return
        val outerSymbol = context.findClosest<FirFunctionSymbol<*>>()

        if (outerSymbol != null && outerSymbol.declares(accessSymbol)) return

        reporter.reportOn(
            expression.source,
            LocalityErrors.INVALID_LOCALITY_CAPTURE,
            outerSymbol ?: FirErrorFunctionSymbol()
        )
    }
}
