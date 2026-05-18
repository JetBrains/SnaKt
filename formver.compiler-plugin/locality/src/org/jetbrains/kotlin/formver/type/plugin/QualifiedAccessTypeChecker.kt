/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.plugin

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType

/**
 * Checker for type compatibility in qualified access expressions.
 *
 * @param Type the type class of the receiver and context arguments.
 * @param typeJudgment the type judgment to use for checking type compatibility.
 * @param expressionTypeResolver the resolver for resolving the actual type of the receiver and context arguments.
 * @param receiverTypeResolver the resolver for resolving the declared type of the receiver.
 * @param variableTypeResolver the resolver for resolving the declared type of the context arguments.
 * @param receiverDiagnosticFactory the diagnostic factory to use for reporting a type mismatch in the receiver.
 * @param contextArgumentDiagnosticFactory the diagnostic factory to use for reporting type mismatch in the context
 *  arguments.
 */
class QualifiedAccessTypeChecker<Type>(
    kind: MppCheckerKind,
    private val typeJudgment: TypeJudgment<Type>,
    private val expressionTypeResolver: ExpressionTypeResolver<Type>,
    private val receiverTypeResolver: SymbolTypeResolver<Type, FirReceiverParameterSymbol>,
    private val variableTypeResolver: SymbolTypeResolver<Type, FirVariableSymbol<*>>,
    private val receiverDiagnosticFactory: KtDiagnosticFactory3<String, Type, Type>,
    private val contextArgumentDiagnosticFactory: KtDiagnosticFactory3<ConeKotlinType, Type, Type>,
) : FirQualifiedAccessExpressionChecker(kind) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        if (expression !is FirFunctionCall && expression !is FirPropertyAccessExpression) return

        val callableSymbol = expression.toResolvedCallableSymbol() ?: return

        val receiverSymbol = callableSymbol.receiverParameterSymbol
        val receiver = expression.extensionReceiver

        if (receiver != null && receiverSymbol != null) {
            val requiredType = receiverTypeResolver.resolveTypeOf(receiverSymbol)
            val actualType = expressionTypeResolver.resolveTypeOf(receiver)

            if (!typeJudgment.satisfies(requiredType, actualType)) {
                reporter.reportOn(
                    receiver.source ?: expression.source,
                    receiverDiagnosticFactory,
                    "Receiver",
                    requiredType,
                    actualType
                )
            }
        }

        if (!LanguageFeature.ContextReceivers.isEnabled() &&
            !LanguageFeature.ContextParameters.isEnabled()
        ) {
            return
        }

        val contextArgumentMappings = expression.contextArguments.zip(callableSymbol.contextParameterSymbols)

        for ((argument, argumentSymbol) in contextArgumentMappings) {
            val requiredType = variableTypeResolver.resolveTypeOf(argumentSymbol)
            val actualType = expressionTypeResolver.resolveTypeOf(argument)

            if (typeJudgment.satisfies(requiredType, actualType)) continue

            reporter.reportOn(
                argument.source ?: expression.source,
                contextArgumentDiagnosticFactory,
                argumentSymbol.resolvedReturnType,
                requiredType,
                actualType
            )
        }
    }
}
