package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessErrors.LEAKED_UNIQUENESS_INCONSISTENCY
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessErrors.CONTEXT_LEAKED_UNIQUENESS_INCONSISTENCY

fun interface StatementLeaksResolver<in Statement : FirStatement> {
    fun resolveLeaksOf(expression: Statement): List<FirExpression>
}

fun interface LeakReporter<in Statement : FirStatement> {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun reportLeak(expression: Statement, leakExpression: FirExpression, leakPath: Path)
}

object DefaultLeakReporter : LeakReporter<FirStatement> {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun reportLeak(
        expression: FirStatement,
        leakExpression: FirExpression,
        leakPath: Path
    ) {
        reporter.reportOn(
            leakExpression.source ?: expression.source,
            LEAKED_UNIQUENESS_INCONSISTENCY,
            leakPath
        )
    }
}

object CallLeakReporter : LeakReporter<FirStatement> {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun reportLeak(expression: FirStatement, leakExpression: FirExpression, leakPath: Path) {
        if (leakExpression.source?.kind is KtFakeSourceElementKind.ImplicitContextParameterArgument) {
            reporter.reportOn(
                expression.source,
                CONTEXT_LEAKED_UNIQUENESS_INCONSISTENCY,
                leakExpression.resolvedType,
                leakPath
            )
        } else {
            DefaultLeakReporter.reportLeak(expression, leakExpression, leakPath)
        }
    }
}

class StatementLeakUniquenessConsistencyChecker<Statement : FirStatement>(
    val statementLeaksResolver: StatementLeaksResolver<Statement>,
    val leakReporter: LeakReporter<Statement> = DefaultLeakReporter
) : FirExpressionChecker<Statement>( MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: Statement) {
        val leaks = statementLeaksResolver.resolveLeaksOf(expression)

        for (leak in leaks) {
            val inputUniquenessState = expression.resolveInputUniquenessState() ?: EmptyUniquenessState
            val leakAccessState = leak.resolveAccessState()

            for (accessPath in leakAccessState.enumeratePaths()) {
                val uniquenessSubstate = inputUniquenessState.find(accessPath) ?: continue
                val movedPaths = uniquenessSubstate.enumerateInconsistentPaths()

                for (movedPath in movedPaths) {
                    leakReporter.reportLeak(expression, leak, accessPath + movedPath)
                }
            }
        }
    }
}

val ReturnBoundaryUniquenessConsistencyChecker = StatementLeakUniquenessConsistencyChecker<FirReturnExpression>(
    statementLeaksResolver = { returnExpression ->
        listOf(returnExpression.result)
    }
)

val ThrowBoundaryUniquenessConsistencyChecker = StatementLeakUniquenessConsistencyChecker<FirThrowExpression>(
    statementLeaksResolver = { throwExpression ->
        listOf(throwExpression.exception)
    }
)

val CallBoundaryUniquenessConsistencyChecker = StatementLeakUniquenessConsistencyChecker<FirCall>(
    statementLeaksResolver = { call ->
        buildList {
            addAll(call.arguments)

            if (call is FirQualifiedAccessExpression) {
                addAll(call.contextArguments)
            }
        }
    },
    leakReporter = CallLeakReporter
)
