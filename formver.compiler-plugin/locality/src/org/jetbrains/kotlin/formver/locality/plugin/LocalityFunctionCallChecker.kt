/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.locality.plugin.LocalityErrors.LOCALITY_VIOLATION
import kotlin.collections.iterator

object LocalityFunctionCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkArgument(argument: FirExpression, requiredArgumentLocality: LocalityAttribute?) {
        val actualArgumentLocality = argument.extractLocality()

        if (requiredArgumentLocality.accepts(actualArgumentLocality)) return

        reporter.reportOn(
            argument.source,
            LOCALITY_VIOLATION,
            "Argument",
            requiredArgumentLocality,
            actualArgumentLocality
        )
    }

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val callableSymbol = expression.toResolvedCallableSymbol() as? FirFunctionSymbol<*>
            ?: return
        val receiverDeclaration = callableSymbol.receiverParameterSymbol?.fir
        val receiver = expression.extensionReceiver

        if (receiver != null && receiverDeclaration != null) {
            val requiredReceiverLocality = receiverDeclaration.extractRequiredLocality()
            val actualReceiverLocality = receiver.extractLocality()

            if (!requiredReceiverLocality.accepts(actualReceiverLocality)) {
                reporter.reportOn(
                    receiver.source ?: expression.source,
                    LOCALITY_VIOLATION,
                    "Receiver",
                    requiredReceiverLocality,
                    actualReceiverLocality
                )
            }
        }

        val argumentMappings = expression.resolvedArgumentMapping
            ?: return

        for ((argument, argumentDeclaration) in argumentMappings) {
            checkArgument(argument, argumentDeclaration.extractRequiredLocality())
        }
    }
}
