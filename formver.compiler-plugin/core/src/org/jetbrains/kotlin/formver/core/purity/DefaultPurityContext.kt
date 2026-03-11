/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.purity

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.ErrorCollector
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding

/**
 * Standard [PurityContext] implementation used during compilation.
 *
 * For each purity violation it resolves the precise source position of the offending
 * [org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding] via
 * [expressionSource] (falling back to [source] when the node carries no position of
 * its own) and forwards the error to [errorCollector].
 *
 * @param source The [KtSourceElement] of the enclosing function, used as a fallback
 *   source location when an embedding node has no source of its own.
 * @param errorCollector Accumulates purity errors for later bulk reporting.
 */
class DefaultPurityContext(
    private val source: KtSourceElement,
    private val errorCollector: ErrorCollector,
) : PurityContext {
    override fun addPurityError(embedding: ExpEmbedding, msg: String) =
        errorCollector.addPurityError(embedding.expressionSource(source), msg)
}