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
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirValueParameterChecker
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol

/**
 * Checker for type compatibility in value parameter initializers.
 *
 * @param Type the type class of the initializer.
 * @param typeJudgment the type judgment to use for checking type compatibility.
 * @param expressionTypeResolver the resolver for resolving the actual type of the initializer.
 * @param parameterDeclaredTypeResolver the resolver for resolving the expected type of the parameter.
 * @param diagnosticFactory the diagnostic factory to use for reporting type mismatch.
 */
class ValueParameterTypeChecker<Type>(
    kind: MppCheckerKind,
    private val typeJudgment: TypeJudgment<Type>,
    private val expressionTypeResolver: ExpressionTypeResolver<Type>,
    private val parameterDeclaredTypeResolver: SymbolTypeResolver<Type, FirValueParameterSymbol>,
    private val diagnosticFactory: KtDiagnosticFactory3<String, Type, Type>,
) : FirValueParameterChecker(kind) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirValueParameter) {
        val defaultValue = declaration.defaultValue ?: return

        val requiredType = parameterDeclaredTypeResolver.resolveTypeOf(declaration.symbol)
        val actualType = expressionTypeResolver.resolveTypeOf(defaultValue)

        if (typeJudgment.satisfies(requiredType, actualType)) return

        reporter.reportOn(
            defaultValue.source ?: declaration.source,
            diagnosticFactory,
            "Initializer",
            requiredType,
            actualType
        )
    }
}
