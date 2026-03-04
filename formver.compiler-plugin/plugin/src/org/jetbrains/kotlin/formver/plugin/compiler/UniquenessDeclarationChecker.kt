/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.formver.common.ErrorCollector
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.uniqueness.UniquenessCheckException
import org.jetbrains.kotlin.formver.uniqueness.UniquenessGraphChecker
import org.jetbrains.kotlin.formver.uniqueness.UniquenessResolver
import org.jetbrains.kotlin.formver.uniqueness.UniquenessTrie

class UniquenessDeclarationChecker(private val session: FirSession, private val config: PluginConfiguration) :

    FirSimpleFunctionChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirSimpleFunction) {
        if (!config.checkUniqueness) return

        try {
            val errorCollector = ErrorCollector()
            val graph = declaration.controlFlowGraphReference?.controlFlowGraph
                ?: error("Control flow graph is null for declaration: ${declaration.name}")
            val resolver = UniquenessResolver(session)
            val initial = UniquenessTrie(resolver)
            val graphChecker = UniquenessGraphChecker(session, initial, errorCollector)
            graphChecker.check(graph)
        } catch (e: UniquenessCheckException) {
            reporter.reportOn(e.source, PluginErrors.UNIQUENESS_VIOLATION, e.message)
        } catch (e: Exception) {
            val error = e.message ?: "No message provided"
            reporter.reportOn(declaration.source, PluginErrors.INTERNAL_ERROR, error)
        }
    }

}