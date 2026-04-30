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
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.expressions.unwrapAndFlattenArgument
import org.jetbrains.kotlin.fir.resolve.isFunctionOrSuspendFunctionInvoke
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.valueParameterTypesWithoutReceivers
import org.jetbrains.kotlin.formver.locality.plugin.LocalityErrors.LOCALITY_VIOLATION

object CallLocalityChecker : FirCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext)
    private fun FirCall.resolveRequiredLocalityForArgument(argumentDeclaration: FirValueParameter): Locality {
        val implicitInvokeLocality = (this as? FirFunctionCall)
            ?.resolveImplicitInvokeRequiredLocalityForArgument(argumentDeclaration)

        return implicitInvokeLocality ?: argumentDeclaration.resolveRequiredLocality()
    }

    context(context: CheckerContext)
    private fun FirFunctionCall.resolveImplicitInvokeRequiredLocalityForArgument(
        argumentDeclaration: FirValueParameter
    ): Locality? {
        val callableSymbol = toResolvedCallableSymbol() as? FirFunctionSymbol<*> ?: return null
        if (!callableSymbol.callableId.isFunctionOrSuspendFunctionInvoke()) return null

        val argumentIndex = callableSymbol.valueParameterSymbols.indexOf(argumentDeclaration.symbol)
        if (argumentIndex < 0) return null

        val receiver = explicitReceiver ?: dispatchReceiver ?: extensionReceiver ?: return null
        val receiverType = receiver.resolvedType.lowerBoundIfFlexible()
        if (!receiverType.isSomeFunctionType(context.session) || receiverType !is ConeClassLikeType) return null

        val parameterType = receiverType.valueParameterTypesWithoutReceivers(context.session)
            .getOrNull(argumentIndex)
            ?: return null

        return parameterType.resolveRequiredLocality()
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCall) {
        val argumentMappings = expression.resolvedArgumentMapping

        for ((argument, argumentDeclaration) in argumentMappings.orEmpty()) {
            val requiredLocality = expression.resolveRequiredLocalityForArgument(argumentDeclaration)
            val effectiveArguments = argument.unwrapAndFlattenArgument(flattenArrays = false)

            for (effectiveArgument in effectiveArguments) {
                val actualLocality = effectiveArgument.resolveLocality()

                if (requiredLocality.accepts(actualLocality)) continue

                reporter.reportOn(
                    effectiveArgument.source ?: argument.source,
                    LOCALITY_VIOLATION,
                    "Argument",
                    requiredLocality,
                    actualLocality
                )
            }
        }
    }
}
