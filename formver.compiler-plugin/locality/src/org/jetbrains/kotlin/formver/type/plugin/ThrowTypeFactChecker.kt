/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirThrowExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression

/**
 * Checker for type-fact compatibility in throw expressions.
 *
 * @param TypeFact the type-fact class of the exception.
 * @param typeFactJudgment the type-fact judgment to use for checking type-fact compatibility.
 * @param expressionTypeFactResolver the resolver for resolving the actual type fact of the exception.
 * @param throwExceptionTypeFactResolver the resolver for resolving the expected type fact of the thrown exception.
 * @param diagnosticFactory the diagnostic factory to use for reporting type-fact mismatch.
 */
class ThrowTypeFactChecker<TypeFact>(
    kind: MppCheckerKind,
    private val typeFactJudgment: TypeFactJudgment<TypeFact>,
    private val expressionTypeFactResolver: ExpressionTypeFactResolver<TypeFact>,
    private val throwExceptionTypeFactResolver: ThrowExceptionTypeFactResolver<TypeFact>,
    private val diagnosticFactory: KtDiagnosticFactory3<String, TypeFact, TypeFact>,
) : FirThrowExpressionChecker(kind) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirThrowExpression) {
        val requiredTypeFact = throwExceptionTypeFactResolver.resolveExceptionTypeFactOf(expression)
        val actualTypeFact = expressionTypeFactResolver.resolveTypeFactOf(expression.exception)

        if (typeFactJudgment.satisfies(requiredTypeFact, actualTypeFact)) return

        reporter.reportOn(
            expression.exception.source,
            diagnosticFactory,
            "Throw",
            requiredTypeFact,
            actualTypeFact
        )
    }
}
