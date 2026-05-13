/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirReturnExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.formver.locality.plugin.LocalityErrors.LOCALITY_CONTRACT_MISMATCH

@OptIn(SymbolInternals::class)
object ReturnLocalityContractChecker : FirReturnExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirReturnExpression) {
        val returnType = expression.target.labeledElement.returnTypeRef.coneTypeSafe<ConeKotlinType>()
            ?: return
        val requiredContract = returnType.resolveLocalityContract(context.session)
        val actualContract = expression.result.resolveLocalityContract()

        if (requiredContract.accept(actualContract)) return

        reporter.reportOn(
            expression.result.source,
            LOCALITY_CONTRACT_MISMATCH,
            "Return",
            requiredContract,
            actualContract
        )
    }
}
