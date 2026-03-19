/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.plugin.compiler.locality.LocalityFunctionCallChecker
import org.jetbrains.kotlin.formver.plugin.compiler.locality.LocalityPropertyChecker
import org.jetbrains.kotlin.formver.plugin.compiler.locality.LocalityVariableAssignmentChecker

class PluginAdditionalCheckers(session: FirSession, config: PluginConfiguration) :
    FirAdditionalCheckersExtension(session) {
    companion object {
        fun getFactory(config: PluginConfiguration): Factory {
            return Factory { session -> PluginAdditionalCheckers(session, config) }
        }
    }

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker>
            get() = setOf(ViperPoweredDeclarationChecker(session, config), UniquenessDeclarationChecker(session, config))

        override val propertyCheckers: Set<FirPropertyChecker>
            get() = setOf(LocalityPropertyChecker(config))
    }

    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker>
            get() = setOf(LocalityVariableAssignmentChecker(config))

        override val functionCallCheckers: Set<FirFunctionCallChecker>
            get() = setOf(LocalityFunctionCallChecker(config))
    }
}
