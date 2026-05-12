/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.formver.locality.plugin.LocalityErrors.CONTEXT_LOCALITY_MISMATCH
import org.jetbrains.kotlin.formver.locality.plugin.LocalityErrors.LOCALITY_MISMATCH

object QualifiedAccessLocalityChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        if (expression !is FirFunctionCall && expression !is FirPropertyAccessExpression) return

        val callableSymbol = expression.toResolvedCallableSymbol() ?: return
        val receiverSymbol = callableSymbol.receiverParameterSymbol
        val receiver = expression.extensionReceiver

        if (receiver != null && receiverSymbol != null) {
            val expectedLocality = receiverSymbol.resolveLocality()
            val actualLocality = receiver.resolveLocality()

            if (!expectedLocality.accepts(actualLocality)) {
                reporter.reportOn(
                    receiver.source ?: expression.source,
                    LOCALITY_MISMATCH,
                    "Receiver",
                    expectedLocality,
                    actualLocality
                )
            }
        }

        if (!LanguageFeature.ContextReceivers.isEnabled() &&
            !LanguageFeature.ContextParameters.isEnabled()
        ) {
            return
        }

        val contextArgumentMappings = expression.contextArguments
            .zip(callableSymbol.contextParameterSymbols)

        for ((argument, argumentSymbol) in contextArgumentMappings) {
            val expectedLocality = argumentSymbol.resolveLocality()
            val actualLocality = argument.resolveLocality()

            if (expectedLocality.accepts(actualLocality)) continue

            reporter.reportOn(
                argument.source ?: expression.source,
                CONTEXT_LOCALITY_MISMATCH,
                argumentSymbol.resolvedReturnType,
                expectedLocality,
                actualLocality
            )
        }
    }
}
