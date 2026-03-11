/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.purity

import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding

/**
 * Abstracts error reporting during structural validity checks on [ExpEmbedding] trees.
 *
 * Implementations are responsible for resolving the source position of an embedding
 * and forwarding the resulting diagnostic to whatever error-reporting mechanism is
 * active for the current compilation phase (e.g. [DefaultPurityContext] routes errors
 * to an [org.jetbrains.kotlin.formver.common.ErrorCollector]).
 */
interface PurityContext {
    /**
     * Records a purity violation for [embedding] with the human-readable message [msg].
     *
     * The implementation is expected to derive the source location from [embedding]
     * (falling back to the enclosing function source when no per-node source is available)
     * and emit an appropriate compiler diagnostic.
     */
    fun addPurityError(embedding: ExpEmbedding, msg: String)
}
