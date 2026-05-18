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
 * Checker for type compatibility in throw expressions.
 *
 * @param Type the type class of the exception.
 * @param typeJudgment the type judgment to use for checking type compatibility.
 * @param expressionTypeResolver the resolver for resolving the actual type of the exception.
 * @param throwExceptionTypeResolver the resolver for resolving the expected type of the thrown exception.
 * @param diagnosticFactory the diagnostic factory to use for reporting type mismatch.
 */
class ThrowTypeChecker<Type>(
    kind: MppCheckerKind,
    private val typeJudgment: TypeJudgment<Type>,
    private val expressionTypeResolver: ExpressionTypeResolver<Type>,
    private val throwExceptionTypeResolver: ThrowExceptionTypeResolver<Type>,
    private val diagnosticFactory: KtDiagnosticFactory3<String, Type, Type>,
) : FirThrowExpressionChecker(kind) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirThrowExpression) {
        val requiredType = throwExceptionTypeResolver.resolveExceptionTypeOf(expression)
        val actualType = expressionTypeResolver.resolveTypeOf(expression.exception)

        if (typeJudgment.satisfies(requiredType, actualType)) return

        reporter.reportOn(
            expression.exception.source,
            diagnosticFactory,
            "Throw",
            requiredType,
            actualType
        )
    }
}
