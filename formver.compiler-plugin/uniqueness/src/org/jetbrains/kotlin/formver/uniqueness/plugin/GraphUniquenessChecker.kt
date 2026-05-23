/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph

/**
 * CFA checker that triggers the uniqueness state mapping computation for the graph.
 *
 * The actual uniqueness checking is done by the standard expression/declaration checkers
 * registered in [UniquenessAdditionalCheckers]. This checker ensures the
 * [GraphUniquenessStateMappingResolver] cache is populated before those checkers run.
 */
object GraphUniquenessChecker : FirControlFlowChecker(MppCheckerKind.Common) {
    context(reporter: DiagnosticReporter, context: CheckerContext)
    override fun analyze(graph: ControlFlowGraph) {
        // Trigger the mapping computation so it is cached for expression-level checkers.
        graph.resolveUniquenessStateMapping()
    }
}
