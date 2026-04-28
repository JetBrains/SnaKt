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
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.formver.locality.plugin.LocalityErrors.LOCALITY_VIOLATION

object CallLocalityChecker : FirCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCall) {
        val argumentMappings = expression.resolvedArgumentMapping

        for ((argument, argumentDeclaration) in argumentMappings.orEmpty()) {
            val requiredLocality = argumentDeclaration.resolveRequiredLocality()
            val actualLocality = argument.resolveLocality()

            if (requiredLocality.accepts(actualLocality)) continue

            reporter.reportOn(
                argument.source,
                LOCALITY_VIOLATION,
                "Argument",
                requiredLocality,
                actualLocality
            )
        }
    }
}
