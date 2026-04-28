/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.locality.plugin.LocalityErrors.LOCALITY_VIOLATION
import kotlin.collections.iterator

object CallLocalityChecker : FirCallChecker(MppCheckerKind.Common) {
    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCall) {
        val callableSymbol = (expression as? FirResolvable)?.toResolvedCallableSymbol() as? FirFunctionSymbol<*>
            ?: return
        val receiverDeclaration = callableSymbol.receiverParameterSymbol?.fir
        val receiver = (expression as? FirQualifiedAccessExpression)?.extensionReceiver

        if (receiver != null && receiverDeclaration != null) {
            val requiredReceiverLocality = receiverDeclaration.resolveRequiredLocality()
            val actualReceiverLocality = receiver.resolveLocality()

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
            val requiredArgumentLocality = argumentDeclaration.resolveRequiredLocality()
            val actualArgumentLocality = argument.resolveLocality()

            if (requiredArgumentLocality.accepts(actualArgumentLocality)) continue

            reporter.reportOn(
                argument.source,
                LOCALITY_VIOLATION,
                "Argument",
                requiredArgumentLocality,
                actualArgumentLocality
            )
        }
    }
}
