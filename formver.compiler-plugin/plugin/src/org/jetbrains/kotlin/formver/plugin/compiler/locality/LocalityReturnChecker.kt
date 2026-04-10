package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirReturnExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.plugin.compiler.PluginErrors.LOCALITY_VIOLATION

class LocalityReturnChecker(
    private val config : PluginConfiguration
) : FirReturnExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirReturnExpression) {
        if (!config.checkLocality) return

        val leftLocality = Locality.Global
        val rightLocality = expression.result.resolvedLocality

        if (leftLocality.accepts(rightLocality)) return

        reporter.reportOn(
            expression.result.source,
            LOCALITY_VIOLATION,
            "Return locality mismatch: expected '${leftLocality.render()}', " +
                    "actual '${rightLocality.render()}'."
        )
    }
}
