package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.plugin.compiler.PluginErrors.LOCALITY_VIOLATION

class LocalityVariableAssignmentChecker(
    private val session: FirSession,
    private val config : PluginConfiguration
) : FirVariableAssignmentChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirVariableAssignment) {
        if (!config.checkLocality) return

        val localAttributes = session.coneLocalAttributes
        val leftLocality = localAttributes[expression.lValue]
        val rightLocality = localAttributes[expression.rValue]

        if (leftLocality.accepts(rightLocality)) return

        reporter.reportOn(
            expression.rValue.source,
            LOCALITY_VIOLATION,
            "Assignment locality mismatch: expected '${leftLocality.render()}', " +
                    "actual '${rightLocality.render()}'."
        )
    }
}