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
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

/**
 * Checker for type-fact compatibility in property initializers.
 *
 * @param TypeFact the type-fact class of the initializer.
 * @param typeFactJudgment the type-fact judgment to use for checking type-fact compatibility.
 * @param expressionTypeFactResolver the resolver for resolving the type fact of the initializer.
 * @param variableTypeFactResolver the resolver for resolving the type fact of the property.
 * @param diagnosticFactory the diagnostic factory to use for reporting type-fact mismatch.
 */
class PropertyTypeFactChecker<TypeFact>(
    kind: MppCheckerKind,
    private val typeFactJudgment: TypeFactJudgment<TypeFact>,
    private val expressionTypeFactResolver: ExpressionTypeFactResolver<TypeFact>,
    private val variableTypeFactResolver: SymbolTypeFactResolver<TypeFact, FirVariableSymbol<*>>,
    private val diagnosticFactory: KtDiagnosticFactory3<String, TypeFact, TypeFact>,
) : FirPropertyChecker(kind) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val initializer = declaration.initializer ?: return

        val requiredTypeFact = variableTypeFactResolver.resolveTypeFactOf(declaration.symbol)
        val actualTypeFact = expressionTypeFactResolver.resolveTypeFactOf(initializer)

        if (typeFactJudgment.satisfies(requiredTypeFact, actualTypeFact)) return

        reporter.reportOn(
            initializer.source,
            diagnosticFactory,
            "Initializer",
            requiredTypeFact,
            actualTypeFact
        )
    }
}
