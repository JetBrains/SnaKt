/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph

/**
 * Per-FIR-expression view of the uniqueness analysis result.
 *
 * Adapter exposed to the Viper conversion so it can ask "is the receiver of this property
 * access unique at the point where we read it?" without depending on the trie internals.
 */
class UniquenessFacts private constructor(
    private val receiverUniqueAt: Map<FirElement, Boolean>,
) {
    /**
     * Returns true if the dispatch (or extension) receiver of [access] holds a value with
     * uniqueness `Active(Unique, _)` immediately before the access executes. Returns false
     * for anything we don't know about — the conversion is expected to fall back to the
     * conservative aliasing-allowed translation in that case.
     */
    fun isReceiverUniqueAt(access: FirPropertyAccessExpression): Boolean =
        receiverUniqueAt[access] ?: false

    companion object {
        /**
         * Runs the uniqueness analyzer over [graph] and collects per-access receiver-uniqueness
         * snapshots. Returns null if the analyzer cannot complete (e.g. throws on a violating
         * function); the plugin-side `UniquenessDeclarationChecker` still reports the diagnostic,
         * we just skip the optimization for this function body.
         */
        fun analyze(session: FirSession, graph: ControlFlowGraph): UniquenessFacts? {
            return try {
                val resolver = UniquenessResolver(session)
                val initial = UniquenessTrie(resolver)
                val facts = UniquenessGraphAnalyzer(resolver, initial).analyze(graph)

                val receiverUniqueAt = mutableMapOf<FirElement, Boolean>()
                for (node in graph.nodes) {
                    val fir = node.fir as? FirPropertyAccessExpression ?: continue
                    val receiverPath = fir.explicitReceiver?.receiverPath ?: continue
                    val trie = facts.flowBefore(node)
                    val nodeData = trie.ensure(receiverPath)
                    val receiverType = nodeData.parentsJoin
                    receiverUniqueAt[fir] = receiverType is UniquenessType.Active &&
                            receiverType.uniqueLevel == UniqueLevel.Unique
                }
                UniquenessFacts(receiverUniqueAt)
            } catch (_: Exception) {
                null
            }
        }
    }
}
