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
    override fun check(valueParameter: FirValueParameter) {
        if (!config.checkLocality) return

        val defaultValue = valueParameter.defaultValue ?: return
        val requiredLocality = valueParameter.requiredLocality
        val actualLocality = defaultValue.resolvedLocality

        if (requiredLocality.accepts(actualLocality)) return

        reporter.reportOn(
            defaultValue.source ?: valueParameter.source,
            LOCALITY_VIOLATION,
            "Initializer locality mismatch: expected '${requiredLocality.render()}', " +
                    "actual '${actualLocality.render()}'."
        )
    }
}
