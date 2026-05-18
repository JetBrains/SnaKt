/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.formver.core.conversion.FreshEntityProducer
import org.jetbrains.kotlin.formver.core.embeddings.expression.AnonymousVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Exp

/**
 * One entry in the function-scope unfolded-predicate list. We track the receiver's Viper
 * variable name (so per-access emit helpers can recognise it) and the matching predicate
 * access (so we can emit a `fold` at every exit point — function epilogue and each early
 * return). Populated by `visitFunctionExp` for `@Unique` parameters; promoted from
 * "registered, not yet unfolded" to "unfolded" on first access.
 */
data class PreUnfoldedReceiver(
    val receiverName: SymbolicName,
    val predicateAccess: Exp.PredicateAccess,
    var unfolded: Boolean = false,
)

class SharedLinearizationState(private val producer: FreshEntityProducer<AnonymousVariableEmbedding, TypeEmbedding>) {
    /**
     * `@Unique` parameters that *could* be unfolded once at function scope. Per-access
     * read/write helpers consult this list; the first access to such a receiver emits the
     * unfold lazily and flips `unfolded`. Function-exit / early-return paths emit a `fold`
     * only for receivers that were actually unfolded (i.e. accessed). Receivers that the
     * function never touches stay un-unfolded — no `unfold pred; fold pred` no-op pair.
     */
    val preUnfoldedReceivers: MutableList<PreUnfoldedReceiver> = mutableListOf()

    fun freshAnonVar(type: TypeEmbedding) = producer.getFresh(type)
}
