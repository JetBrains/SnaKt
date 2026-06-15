/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirReturnExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirThrowExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class UniquenessAdditionalCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> UniquenessAdditionalCheckers(session) }
        }
    }

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val propertyCheckers: Set<FirPropertyChecker> =
            setOf(PropertyUniquenessChecker)
    }


    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker> =
            setOf(AssignmentUniquenessChecker)

        override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker> =
            setOf(PropertyAccessUniquenessChecker)

        override val callCheckers: Set<FirCallChecker> =
            setOf(CallUniquenessChecker, CallLeakedUniquenessConsistencyChecker)

        override val returnExpressionCheckers: Set<FirReturnExpressionChecker> =
            setOf(ReturnUniquenessChecker, ReturnLeakedUniquenessConsistencyChecker)

        override val throwExpressionCheckers: Set<FirThrowExpressionChecker> =
            setOf(ThrowUniquenessChecker, ThrowLeakedUniquenessConsistencyChecker)
    }
}
