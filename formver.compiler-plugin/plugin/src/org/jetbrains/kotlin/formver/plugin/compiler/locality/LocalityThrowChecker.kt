package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirThrowExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.plugin.compiler.PluginErrors.LOCALITY_VIOLATION

class LocalityThrowChecker(
    private val config : PluginConfiguration
) : FirThrowExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirThrowExpression) {
        if (!config.checkLocality) return

        val leftLocality: ConeLocalAttribute? = null
        val rightLocality = expression.exception.resolvedLocalAttribute

        if (leftLocality.accepts(rightLocality)) return

        reporter.reportOn(
            expression.exception.source,
            LOCALITY_VIOLATION,
            "Throw locality mismatch: expected '${leftLocality.render()}', " +
                    "actual '${rightLocality.render()}'."
        )
    }
}
