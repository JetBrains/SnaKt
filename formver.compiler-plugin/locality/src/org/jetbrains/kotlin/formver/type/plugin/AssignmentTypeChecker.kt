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
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment

/**
 * Checker for type compatibility in variable assignments.
 *
 * @param Type the type class of the variables.
 * @param typeJudgment the type judgment to use for checking type compatibility.
 * @param expressionTypeResolver the resolver for resolving the types of both the left and right-hand expressions of
 *  the assignment.
 * @param diagnosticFactory the diagnostic factory to use for reporting type incompatibility.
 */
class AssignmentTypeChecker<Type>(
    kind: MppCheckerKind,
    private val typeJudgment: TypeJudgment<Type>,
    private val expressionTypeResolver: ExpressionTypeResolver<Type>,
    private val diagnosticFactory: KtDiagnosticFactory3<String, Type, Type>
) : FirVariableAssignmentChecker(kind) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirVariableAssignment) {
        val requiredType = expressionTypeResolver.resolveTypeOf(expression.lValue)
        val actualType = expressionTypeResolver.resolveTypeOf(expression.rValue)

        if (typeJudgment.satisfies(requiredType, actualType)) return

        reporter.reportOn(
            expression.rValue.source,
            diagnosticFactory,
            "Assignment",
            requiredType,
            actualType
        )
    }
}
