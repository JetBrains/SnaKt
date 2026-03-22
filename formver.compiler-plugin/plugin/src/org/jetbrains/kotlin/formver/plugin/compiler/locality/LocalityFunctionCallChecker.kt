package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.plugin.compiler.PluginErrors.LOCALITY_VIOLATION

class LocalityFunctionCallChecker(
    private val session: FirSession,
    private val config : PluginConfiguration
) : FirFunctionCallChecker(MppCheckerKind.Common) {
    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        if (!config.checkLocality) return
        val localAttributes = session.coneLocalAttributes

        val callableSymbol = expression.toResolvedCallableSymbol() as? FirFunctionSymbol<*>
            ?: throw IllegalStateException("Unable to resolve ${expression}")
        val parameters = callableSymbol.valueParameterSymbols
        val arguments = expression.arguments

        for ((parameter, argument) in parameters.zip(arguments)) {
            val parameterLocality = parameter.defaultType.localAttribute
            val argumentLocality = localAttributes[argument]

            if (parameterLocality.accepts(argumentLocality)) continue

            reporter.reportOn(
                argument.source,
                LOCALITY_VIOLATION,
                "Argument uniqueness mismatch: expected '${parameterLocality.render()}', " +
                        "actual '${argumentLocality.render()}'."
            )
        }
    }
}
