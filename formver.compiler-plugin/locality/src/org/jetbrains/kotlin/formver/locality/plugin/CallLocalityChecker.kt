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
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.expressions.unwrapAndFlattenArgument
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.formver.locality.plugin.LocalityErrors.LOCALITY_VIOLATION

object CallLocalityChecker : FirCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext)
    private fun FirCall.collectRequiredLocalities(): Map<FirExpression, LocalityAttribute?>? {
        if (this is FirImplicitInvokeCall) {
            val contract = dispatchReceiver?.resolveLocalityContract()

            if (contract != null) {
                val invokeArguments = listOfNotNull(extensionReceiver) + arguments

                return invokeArguments.zip(contract).toMap()
            }
        }

        return resolvedArgumentMapping?.map { (argument, argumentDeclaration) ->
            argument to argumentDeclaration.returnTypeRef.coneType.locality
        }?.toMap()
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCall) {
        val requiredLocalities = expression.collectRequiredLocalities()

        for ((argument, requiredLocality) in requiredLocalities.orEmpty()) {
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
