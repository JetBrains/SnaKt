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
 * Checker for type-fact compatibility in value parameter initializers.
 *
 * @param TypeFact the type-fact class of the initializer.
 * @param typeFactJudgment the type-fact judgment to use for checking type-fact compatibility.
 * @param expressionTypeFactResolver the resolver for resolving the actual type fact of the initializer.
 * @param parameterDeclaredTypeFactResolver the resolver for resolving the expected type fact of the parameter.
 * @param diagnosticFactory the diagnostic factory to use for reporting type-fact mismatch.
 */
class ValueParameterTypeFactChecker<TypeFact>(
    kind: MppCheckerKind,
    private val typeFactJudgment: TypeFactJudgment<TypeFact>,
    private val expressionTypeFactResolver: ExpressionTypeFactResolver<TypeFact>,
    private val parameterDeclaredTypeFactResolver: SymbolTypeFactResolver<TypeFact, FirValueParameterSymbol>,
    private val diagnosticFactory: KtDiagnosticFactory3<String, TypeFact, TypeFact>,
) : FirValueParameterChecker(kind) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirValueParameter) {
        val defaultValue = declaration.defaultValue ?: return

        val requiredTypeFact = parameterDeclaredTypeFactResolver.resolveTypeFactOf(declaration.symbol)
        val actualTypeFact = expressionTypeFactResolver.resolveTypeFactOf(defaultValue)

        if (typeFactJudgment.satisfies(requiredTypeFact, actualTypeFact)) return

        reporter.reportOn(
            defaultValue.source ?: declaration.source,
            diagnosticFactory,
            "Initializer",
            requiredTypeFact,
            actualTypeFact
        )
    }
}
