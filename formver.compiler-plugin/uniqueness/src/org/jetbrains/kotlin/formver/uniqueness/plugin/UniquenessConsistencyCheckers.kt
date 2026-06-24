package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessErrors.UNIQUENESS_INCONSISTENCY

fun interface StatementLeaksResolver<Statement : FirStatement> {
    fun resolveLeaks(expression: Statement): List<FirExpression>
}

class StatementUniquenessConsistencyChecker<Statement : FirStatement>(
    val statementLeaksResolver: StatementLeaksResolver<Statement>
) : FirExpressionChecker<Statement>( MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: Statement) {
        val leaks = statementLeaksResolver.resolveLeaks(expression)

        for (leak in leaks) {
            val inputUniquenessState = leak.resolveInputUniquenessState() ?: EmptyUniquenessState
            val leakAccessState = leak.resolveAccessState()

            for (accessPath in leakAccessState.enumerateTerminalPaths()) {
                val uniquenessSubstate = inputUniquenessState.find(accessPath) ?: continue
                val movedPaths = uniquenessSubstate.enumerateInconsistentPaths()

                for (movedPath in movedPaths) {
                    reporter.reportOn(
                        leak.source ?: expression.source,
                        UNIQUENESS_INCONSISTENCY,
                        accessPath + movedPath
                    )
                }
            }
        }
    }
}

val ReturnLeakedUniquenessConsistencyChecker = StatementUniquenessConsistencyChecker<FirReturnExpression>(
    statementLeaksResolver = { returnExpression ->
        listOf(returnExpression.result)
    }
)

val ThrowLeakedUniquenessConsistencyChecker = StatementUniquenessConsistencyChecker<FirThrowExpression>(
    statementLeaksResolver = { throwExpression ->
        listOf(throwExpression.exception)
    }
)

val CallLeakedUniquenessConsistencyChecker = StatementUniquenessConsistencyChecker<FirCall>(
    statementLeaksResolver = { call ->
        call.arguments
    }
)
