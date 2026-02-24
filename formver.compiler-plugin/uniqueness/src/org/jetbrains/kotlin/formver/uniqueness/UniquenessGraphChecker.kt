/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.formver.common.ErrorCollector

class UniquenessGraphChecker(
    val session: FirSession,
    val initial: Map<Path, UniquenessType>,
    val out: ErrorCollector
) {

    fun check(graph: ControlFlowGraph) {
        val resolver = UniquenessResolver(session)
        val analyzer = UniquenessGraphAnalyzer(resolver, initial)
        val facts = analyzer.analyze(graph)
        val expressionChecker = UniquenessExpressionChecker(resolver, out)

        for (node in graph.nodes) {
            val store = facts.flowBefore(node)
            expressionChecker.check(store, node.fir)
        }
    }

}