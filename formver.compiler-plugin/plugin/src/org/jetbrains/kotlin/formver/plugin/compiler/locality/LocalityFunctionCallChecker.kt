package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.plugin.compiler.PluginErrors.LOCALITY_VIOLATION

class LocalityFunctionCallChecker(
    private val config : PluginConfiguration
) : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkArgument(argument: org.jetbrains.kotlin.fir.expressions.FirExpression, requiredArgumentLocality: Locality) {
        val actualArgumentLocality = argument.resolvedLocality

        if (requiredArgumentLocality.accepts(actualArgumentLocality)) return

        reporter.reportOn(
            argument.source,
            LOCALITY_VIOLATION,
            "Argument locality mismatch: expected '${requiredArgumentLocality.render()}', " +
                    "actual '${actualArgumentLocality.render()}'."
        )
    }

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(functionCall: FirFunctionCall) {
        if (!config.checkLocality) return

        val callableSymbol = functionCall.toResolvedCallableSymbol() as? FirFunctionSymbol<*>
            ?: throw IllegalStateException("Unable to resolve $functionCall")
        val receiverDeclaration = callableSymbol.receiverParameterSymbol?.fir
        val receiver = functionCall.extensionReceiver

        if (receiver != null && receiverDeclaration != null) {
            val requiredReceiverLocality = receiverDeclaration.requiredLocality
            val actualReceiverLocality = receiver.resolvedLocality

            if (!requiredReceiverLocality.accepts(actualReceiverLocality)) {
                reporter.reportOn(
                    receiver.source ?: functionCall.source,
                    LOCALITY_VIOLATION,
                    "Receiver locality mismatch: expected '${requiredReceiverLocality.render()}', " +
                            "actual '${actualReceiverLocality.render()}'."
                )
            }
        }

        val resolvedArgumentMapping = functionCall.resolvedArgumentMapping
        val argumentMappings = if (resolvedArgumentMapping != null) {
            resolvedArgumentMapping.toList()
        } else {
            val argumentSymbols = callableSymbol.valueParameterSymbols
            val arguments = functionCall.arguments

            arguments.zip(argumentSymbols.map { it.fir })
        }

        for ((argument, argumentDeclaration) in argumentMappings) {
            checkArgument(argument, argumentDeclaration.requiredLocality)
        }
    }

}
