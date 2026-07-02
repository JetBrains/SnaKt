package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.formver.locality.plugin.Locality
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessErrors.INVALID_LEAKED_UNIQUENESS


class StatementLeakUniquenessChecker<Statement : FirStatement>(
    val statementLeaksResolver: StatementLeaksResolver<Statement>,
) : FirExpressionChecker<Statement>( MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: Statement) {
        val leaks = statementLeaksResolver.resolveLeaksOf(expression)

        for (leak in leaks) {
            val leakAccessState = leak.resolveAccessState()

            for (accessPath in leakAccessState.enumeratePaths()) {
                if (accessPath.firstOrNull()?.locality == Locality.Local && accessPath.uniqueness <= Uniqueness.Unknown) {
                    reporter.reportOn(expression.source, INVALID_LEAKED_UNIQUENESS, accessPath.uniqueness, accessPath)
                }
            }
        }
    }
}

val ReturnLeakedUniquenessChecker = StatementLeakUniquenessChecker<FirReturnExpression>(
    statementLeaksResolver = { returnExpression ->
        listOf(returnExpression.result)
    }
)

val ThrowLeakedUniquenessConsistencyChecker = StatementLeakUniquenessChecker<FirThrowExpression>(
    statementLeaksResolver = { throwExpression ->
        listOf(throwExpression.exception)
    }
)

val CallLeakedUniquenessConsistencyChecker = StatementLeakUniquenessChecker<FirCall>(
    statementLeaksResolver = { call ->
        buildList {
            addAll(call.arguments)

            if (call is FirQualifiedAccessExpression) {
                addAll(call.contextArguments)
            }
        }
    }
)
