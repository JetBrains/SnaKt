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
            ?: throw IllegalStateException("Unable to resolve $expression")
        val receiverDeclaration = callableSymbol.receiverParameterSymbol?.fir
        val receiver = expression.explicitReceiver

        if (receiver != null && receiverDeclaration != null) {
            val expectedReceiverLocality = receiverDeclaration.requiredLocality
            val actualReceiverLocality = receiver.localityAttribute

            if (!expectedReceiverLocality.accepts(actualReceiverLocality)) {
                reporter.reportOn(
                    receiver.source ?: expression.source,
                    LOCALITY_VIOLATION,
                    "Receiver uniqueness mismatch: expected '${requiredReceiverLocality.render()}', " +
                            "actual '${actualReceiverLocality.render()}'."
                )
            }
        }

        val argumentSymbols = callableSymbol.valueParameterSymbols
        val arguments = expression.arguments

        for ((argumentSymbol, argument) in argumentSymbols.zip(arguments)) {
            val requiredArgumentLocality = argumentSymbol.fir.requiredLocality
            val actualArgumentLocality = argument.resolvedLocality

            if (requiredArgumentLocality.accepts(actualArgumentLocality)) continue

            reporter.reportOn(
                argument.source,
                LOCALITY_VIOLATION,
                "Argument uniqueness mismatch: expected '${requiredArgumentLocality.render()}', " +
                        "actual '${actualArgumentLocality.render()}'."
            )
        }
    }
}
