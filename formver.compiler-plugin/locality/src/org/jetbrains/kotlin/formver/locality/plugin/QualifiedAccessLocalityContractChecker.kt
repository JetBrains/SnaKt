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
import org.jetbrains.kotlin.formver.locality.plugin.LocalityErrors.LOCALITY_CONTRACT_MISMATCH

object QualifiedAccessLocalityContractChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        if (expression !is FirFunctionCall && expression !is FirPropertyAccessExpression) return

        val callableSymbol = expression.toResolvedCallableSymbol() ?: return
        val receiverSymbol = callableSymbol.receiverParameterSymbol
        val receiver = expression.extensionReceiver

        if (receiver != null && receiverSymbol != null) {
            val requiredContract = receiverSymbol.resolveLocalityContract()
            val actualContract = receiver.resolveLocalityContract()

            if (!requiredContract.accept(actualContract)) {
                reporter.reportOn(
                    receiver.source ?: expression.source,
                    LOCALITY_CONTRACT_MISMATCH,
                    "Receiver",
                    requiredContract,
                    actualContract
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
            val requiredContract = argumentSymbol.resolveLocalityContract()
            val actualContract = argument.resolveLocalityContract()

            if (requiredContract.accept(actualContract)) continue

            reporter.reportOn(
                argument.source ?: expression.source,
                LOCALITY_CONTRACT_MISMATCH,
                "Receiver",
                requiredContract,
                actualContract
            )
        }
    }
}
