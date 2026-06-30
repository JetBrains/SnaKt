package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirStatement
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

class ExitStatementUniquenessChecker
    : FirExpressionChecker<FirStatement>( MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirStatement) {
        val inputUniquenessState = expression.resolveInputUniquenessState()
        val topLevelUniquenessStates = inputUniquenessState?.children ?: emptyMap()

        for ((symbol, uniquenessState) in topLevelUniquenessStates) {
            if (symbol.locality == LocalityAttribute) {
                val inconsistentPaths = uniquenessState.enumerateInconsistentPaths()

                for (inconsistentPath in inconsistentPaths) {
                    reporter.reportOn(
                        expression.source,
                        CAPTURED_UNIQUENESS_INCONSISTENCY,
                        inconsistentPath
                    )
                }
            }
        }
    }
}
