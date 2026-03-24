package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.plugin.compiler.PluginErrors.LOCALITY_VIOLATION

class LocalityPropertyChecker(
    private val config : PluginConfiguration
) : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        if (!config.checkLocality) return

        val initializer = declaration.initializer ?: return
        val leftLocality = declaration.returnTypeRef.coneType.localAttribute
        val rightLocality = initializer.resolvedLocalAttribute

        if (leftLocality.accepts(rightLocality)) return

        reporter.reportOn(
            initializer.source,
            LOCALITY_VIOLATION,
            "Assignment locality mismatch: expected '${leftLocality.render()}', " +
                    "actual '${rightLocality.render()}'."
        )
    }
}
