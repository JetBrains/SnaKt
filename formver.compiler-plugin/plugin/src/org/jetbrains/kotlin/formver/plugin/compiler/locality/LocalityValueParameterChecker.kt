/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirValueParameterChecker
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.plugin.compiler.PluginErrors.LOCALITY_VIOLATION

class LocalityValueParameterChecker(
    private val config : PluginConfiguration
) : FirValueParameterChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirValueParameter) {
        if (!config.checkLocality) return

        val defaultValue = declaration.defaultValue ?: return
        val requiredLocality = declaration.extractRequiredLocality()
        val actualLocality = defaultValue.extractLocality()

        if (requiredLocality.accepts(actualLocality)) return

        reporter.reportOn(
            defaultValue.source ?: declaration.source,
            LOCALITY_VIOLATION,
            "Initializer",
            requiredLocality,
            actualLocality
        )
    }
}
