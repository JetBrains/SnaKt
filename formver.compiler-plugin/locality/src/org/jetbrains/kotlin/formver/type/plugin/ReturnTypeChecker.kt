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
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirReturnExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression

/**
 * Checker for type compatibility in return expressions.
 *
 * @param Type the type class of the return value.
 * @param typeJudgment the type judgment to use for checking type compatibility.
 * @param expressionTypeResolver the resolver for resolving the actual type of the return value.
 * @param returnResultTypeResolver the resolver for resolving the expected type of the return expression.
 * @param diagnosticFactory the diagnostic factory to use for reporting type mismatch.
 */
class ReturnTypeChecker<Type>(
    kind: MppCheckerKind,
    private val typeJudgment: TypeJudgment<Type>,
    private val expressionTypeResolver: ExpressionTypeResolver<Type>,
    private val returnResultTypeResolver: ReturnResultTypeResolver<Type>,
    private val diagnosticFactory: KtDiagnosticFactory3<String, Type, Type>,
) : FirReturnExpressionChecker(kind) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirReturnExpression) {
        val requiredType = returnResultTypeResolver.resolveResultTypeOf(expression)
        val actualType = expressionTypeResolver.resolveTypeOf(expression.result)

        if (typeJudgment.satisfies(requiredType, actualType)) return

        reporter.reportOn(
            expression.result.source,
            diagnosticFactory,
            "Return",
            requiredType,
            actualType
        )
    }
}
