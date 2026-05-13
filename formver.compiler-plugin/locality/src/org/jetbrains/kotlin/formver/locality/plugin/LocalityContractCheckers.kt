/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirValueParameterChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirReturnExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.expressions.unwrapAndFlattenArgument
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.formver.locality.plugin.LocalityErrors.LOCALITY_CONTRACT_MISMATCH

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun reportLocalityContractMismatch(
    source: KtSourceElement?,
    kind: String,
    expectedContract: LocalityContract,
    actualContract: LocalityContract,
) {
    if (expectedContract.accept(actualContract)) return

    reporter.reportOn(
        source,
        LOCALITY_CONTRACT_MISMATCH,
        kind,
        expectedContract,
        actualContract
    )
}

object PropertyLocalityContractChecker : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val initializer = declaration.initializer ?: return
        val expectedContract = declaration.symbol.resolveLocalityContract()
        val actualContract = initializer.resolveLocalityContract()

        reportLocalityContractMismatch(
            initializer.source,
            "Initializer",
            expectedContract,
            actualContract
        )
    }
}

object ValueParameterLocalityContractChecker : FirValueParameterChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirValueParameter) {
        val defaultValue = declaration.defaultValue ?: return
        val expectedContract = declaration.symbol.resolveLocalityContract()
        val actualContract = defaultValue.resolveLocalityContract()

        reportLocalityContractMismatch(
            defaultValue.source ?: declaration.source,
            "Initializer",
            expectedContract,
            actualContract
        )
    }
}

object AssignmentLocalityContractChecker : FirVariableAssignmentChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirVariableAssignment) {
        val expectedContract = expression.lValue.resolveLocalityContract()
        val actualContract = expression.rValue.resolveLocalityContract()

        reportLocalityContractMismatch(
            expression.rValue.source,
            "Assignment",
            expectedContract,
            actualContract
        )
    }
}

object CallLocalityContractChecker : FirCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCall) {
        for ((argument, argumentDeclaration) in expression.resolvedArgumentMapping.orEmpty()) {
            val expectedContract = argumentDeclaration.symbol.resolveLocalityContract()

            for (effectiveArgument in argument.unwrapAndFlattenArgument(flattenArrays = false)) {
                val actualContract = effectiveArgument.resolveLocalityContract()

                reportLocalityContractMismatch(
                    effectiveArgument.source ?: argument.source,
                    "Argument",
                    expectedContract,
                    actualContract
                )
            }
        }
    }
}

object QualifiedAccessLocalityContractChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        if (expression !is FirFunctionCall && expression !is FirPropertyAccessExpression) return

        val callableSymbol = expression.toResolvedCallableSymbol() ?: return
        val receiverSymbol = callableSymbol.receiverParameterSymbol
        val receiver = expression.extensionReceiver

        if (receiver != null && receiverSymbol != null) {
            val expectedContract = receiverSymbol.resolveLocalityContract()
            val actualContract = receiver.resolveLocalityContract()

            reportLocalityContractMismatch(
                receiver.source ?: expression.source,
                "Receiver",
                expectedContract,
                actualContract
            )
        }

        if (!LanguageFeature.ContextReceivers.isEnabled() &&
            !LanguageFeature.ContextParameters.isEnabled()
        ) {
            return
        }

        val contextArgumentMappings = expression.contextArguments
            .zip(callableSymbol.contextParameterSymbols)

        for ((argument, argumentSymbol) in contextArgumentMappings) {
            val expectedContract = argumentSymbol.resolveLocalityContract()
            val actualContract = argument.resolveLocalityContract()

            reportLocalityContractMismatch(
                argument.source ?: expression.source,
                "Context argument",
                expectedContract,
                actualContract
            )
        }
    }
}

@OptIn(SymbolInternals::class)
object ReturnLocalityContractChecker : FirReturnExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirReturnExpression) {
        val returnType = expression.target.labeledElement.returnTypeRef.coneTypeSafe<ConeKotlinType>()
            ?: return
        val expectedContract = returnType.resolveLocalityContract(context.session)
        val actualContract = expression.result.resolveLocalityContract()

        reportLocalityContractMismatch(
            expression.result.source,
            "Return",
            expectedContract,
            actualContract
        )
    }
}
