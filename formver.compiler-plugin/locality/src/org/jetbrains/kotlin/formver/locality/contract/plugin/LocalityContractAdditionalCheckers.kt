/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.contract.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirValueParameterChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirReturnExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class LocalityContractAdditionalCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> LocalityContractAdditionalCheckers(session) }
        }
    }

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val propertyCheckers: Set<FirPropertyChecker> =
            setOf(PropertyLocalityContractChecker)

        override val valueParameterCheckers: Set<FirValueParameterChecker> =
            setOf(ValueParameterLocalityContractChecker)
    }

    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker> =
            setOf(AssignmentLocalityContractChecker)

        override val callCheckers: Set<FirCallChecker> =
            setOf(CallLocalityContractChecker)

        override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker> =
            setOf(QualifiedAccessLocalityContractChecker)

        override val returnExpressionCheckers: Set<FirReturnExpressionChecker> =
            setOf(ReturnLocalityContractChecker)
    }
}
