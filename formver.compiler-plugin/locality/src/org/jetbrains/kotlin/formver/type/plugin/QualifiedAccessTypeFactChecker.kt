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
 * Checker for type-fact compatibility in qualified access expressions.
 *
 * @param TypeFact the type-fact class of the receiver and context arguments.
 * @param typeFactJudgment the type-fact judgment to use for checking type-fact compatibility.
 * @param expressionTypeFactResolver the resolver for resolving the actual type fact of the receiver and context
 *  arguments.
 * @param receiverTypeFactResolver the resolver for resolving the declared type fact of the receiver.
 * @param variableTypeFactResolver the resolver for resolving the declared type fact of the context arguments.
 * @param receiverDiagnosticFactory the diagnostic factory to use for reporting a type-fact mismatch in the receiver.
 * @param contextArgumentDiagnosticFactory the diagnostic factory to use for reporting type-fact mismatch in the context
 *  arguments.
 */
class QualifiedAccessTypeFactChecker<TypeFact>(
    kind: MppCheckerKind,
    private val typeFactJudgment: TypeFactJudgment<TypeFact>,
    private val expressionTypeFactResolver: ExpressionTypeFactResolver<TypeFact>,
    private val receiverTypeFactResolver: SymbolTypeFactResolver<TypeFact, FirReceiverParameterSymbol>,
    private val variableTypeFactResolver: SymbolTypeFactResolver<TypeFact, FirVariableSymbol<*>>,
    private val receiverDiagnosticFactory: KtDiagnosticFactory3<String, TypeFact, TypeFact>,
    private val contextArgumentDiagnosticFactory: KtDiagnosticFactory3<ConeKotlinType, TypeFact, TypeFact>,
) : FirQualifiedAccessExpressionChecker(kind) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        if (expression !is FirFunctionCall && expression !is FirPropertyAccessExpression) return

        val callableSymbol = expression.toResolvedCallableSymbol() ?: return

        val receiverSymbol = callableSymbol.receiverParameterSymbol
        val receiver = expression.extensionReceiver

        if (receiver != null && receiverSymbol != null) {
            val requiredTypeFact = receiverTypeFactResolver.resolveTypeFactOf(receiverSymbol)
            val actualTypeFact = expressionTypeFactResolver.resolveTypeFactOf(receiver)

            if (!typeFactJudgment.satisfies(requiredTypeFact, actualTypeFact)) {
                reporter.reportOn(
                    receiver.source ?: expression.source,
                    receiverDiagnosticFactory,
                    "Receiver",
                    requiredTypeFact,
                    actualTypeFact
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
            val requiredTypeFact = variableTypeFactResolver.resolveTypeFactOf(argumentSymbol)
            val actualTypeFact = expressionTypeFactResolver.resolveTypeFactOf(argument)

            if (typeFactJudgment.satisfies(requiredTypeFact, actualTypeFact)) continue

            reporter.reportOn(
                argument.source ?: expression.source,
                contextArgumentDiagnosticFactory,
                argumentSymbol.resolvedReturnType,
                requiredTypeFact,
                actualTypeFact
            )
        }
    }
}
