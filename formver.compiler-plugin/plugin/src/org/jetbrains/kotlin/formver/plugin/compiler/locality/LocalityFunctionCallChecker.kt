package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
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
    private val config : PluginConfiguration
) : FirFunctionCallChecker(MppCheckerKind.Common) {
    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        if (!config.checkLocality) return

        val callableSymbol = expression.toResolvedCallableSymbol() as? FirFunctionSymbol<*>
            ?: throw IllegalStateException("Unable to resolve ${expression}")
        val receiverDeclaration = callableSymbol.receiverParameterSymbol?.fir
        val receiver = expression.dispatchReceiver

        if (receiver != null && receiverDeclaration != null) {
            val expectedReceiverLocality = receiverDeclaration.localityAttribute
            val actualReceiverLocality = receiver.localityAttribute

            if (!actualReceiverLocality.accepts(expectedReceiverLocality)) {
                reporter.reportOn(
                    receiver.source ?: expression.source,
                    LOCALITY_VIOLATION,
                    "Receiver uniqueness mismatch: expected '${expectedReceiverLocality.render()}', " +
                            "actual '${actualReceiverLocality.render()}'."
                )
            }
        }

        for ((parameter, argument) in parameters.zip(arguments)) {
            val parameterLocality = parameter.resolvedReturnType.localAttribute
            val argumentLocality = argument.resolvedLocalAttribute

            if (parameterLocality.accepts(argumentLocality)) continue

            reporter.reportOn(
            if (expectedArgumentLocality.accepts(actualArgumentLocality)) continue
                LOCALITY_VIOLATION,
                "Argument uniqueness mismatch: expected '${parameterLocality.render()}', " +
                        "actual '${argumentLocality.render()}'."
            )
                "Argument uniqueness mismatch: expected '${expectedArgumentLocality.render()}', " +
                        "actual '${actualArgumentLocality.render()}'."
}
