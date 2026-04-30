/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirValueParameterChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirReturnExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirThrowExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class LocalityAdditionalCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> LocalityAdditionalCheckers(session) }
        }
    }

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val propertyCheckers: Set<FirPropertyChecker>
            get() = setOf(PropertyLocalityChecker)

        override val valueParameterCheckers: Set<FirValueParameterChecker>
            get() = setOf(ValueParameterLocalityChecker)
    }

    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker>
            get() = setOf(VariableAssignmentLocalityChecker)

        override val callCheckers: Set<FirCallChecker>
            get() = setOf(CallLocalityChecker)

        override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker>
            get() = setOf(QualifiedAccessLocalityChecker)

        override val returnExpressionCheckers: Set<FirReturnExpressionChecker>
            get() = setOf(ReturnLocalityChecker)

        override val throwExpressionCheckers: Set<FirThrowExpressionChecker>
            get() = setOf(ThrowLocalityChecker)
    }

    override val typeCheckers: TypeCheckers = object : TypeCheckers() {
        override val resolvedTypeRefCheckers: Set<FirResolvedTypeRefChecker>
            get() = setOf(TypeLocalityAttributeChecker)
    }
}
