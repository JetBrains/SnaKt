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
 * Checker for type compatibility in property initializers.
 *
 * @param Type the type class of the initializer.
 * @param typeJudgment the type judgment to use for checking type compatibility.
 * @param expressionTypeResolver the resolver for resolving the type of the initializer.
 * @param variableTypeResolver the resolver for resolving the type of the property.
 * @param diagnosticFactory the diagnostic factory to use for reporting type mismatch.
 */
class PropertyTypeChecker<Type>(
    kind: MppCheckerKind,
    private val typeJudgment: TypeJudgment<Type>,
    private val expressionTypeResolver: ExpressionTypeResolver<Type>,
    private val variableTypeResolver: SymbolTypeResolver<Type, FirVariableSymbol<*>>,
    private val diagnosticFactory: KtDiagnosticFactory3<String, Type, Type>,
) : FirPropertyChecker(kind) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val initializer = declaration.initializer ?: return

        val requiredType = variableTypeResolver.resolveTypeOf(declaration.symbol)
        val actualType = expressionTypeResolver.resolveTypeOf(initializer)

        if (typeJudgment.satisfies(requiredType, actualType)) return

        reporter.reportOn(
            initializer.source,
            diagnosticFactory,
            "Assignment",
            requiredType,
            actualType
        )
    }
}
