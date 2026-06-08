package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.allReceiverExpressions

object PropertyAccessUniquenessChecker : FirPropertyAccessExpressionChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirPropertyAccessExpression) {
        for (receiver in expression.allReceiverExpressions) {
            val receiverUniqueness = receiver.resolveUniqueness()

            if (receiverUniqueness == Uniqueness.Moved) {
                reporter.reportOn(receiver.source, UniquenessErrors.INVALID_MOVED_ACCESS)
            }
        }
    }

}
