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
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.expressions.unwrapAndFlattenArgument
import org.jetbrains.kotlin.formver.locality.plugin.LocalityErrors.LOCALITY_VIOLATION

object CallLocalityChecker : FirCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCall) {
        if (expression is FirImplicitInvokeCall && expression.checkImplicitInvokeContract()) return

        val argumentMappings = expression.resolvedArgumentMapping

        for ((argument, argumentDeclaration) in argumentMappings.orEmpty()) {
            val localityRequirement = argumentDeclaration.returnTypeRef.resolveLocalityRequirement()
            checkArgumentLocality(argument, localityRequirement)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun FirImplicitInvokeCall.checkImplicitInvokeContract(): Boolean {
        val contract = dispatchReceiver?.resolveLocalityContract() as? ActiveContract
            ?: return false
        val arguments = argumentList.arguments

        if (contract.requiredLocalities.size != arguments.size) return false

        for ((argument, localityRequirement) in arguments.zip(contract.requiredLocalities)) {
            checkArgumentLocality(argument, localityRequirement)
        }

        return true
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkArgumentLocality(
        argument: FirExpression,
        localityRequirement: LocalityRequirement,
    ) {
        val effectiveArguments = argument.unwrapAndFlattenArgument(flattenArrays = false)

        for (effectiveArgument in effectiveArguments) {
            val actualLocality = effectiveArgument.resolveLocality()

            if (localityRequirement.accepts(actualLocality)) continue

            reporter.reportOn(
                effectiveArgument.source ?: argument.source,
                LOCALITY_VIOLATION,
                "Argument",
                localityRequirement.generateWitness(),
                actualLocality
            )
        }
    }
}
