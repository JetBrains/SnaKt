/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.purity

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.conversion.ProgramConversionContext
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding

class DefaultPurityContext(
    private val source: KtSourceElement,
    private val ctx: ProgramConversionContext,
) : PurityContext {
    override fun addPurityError(embedding: ExpEmbedding, msg: String) =
        ctx.reportPurityViolation(embedding.expressionSource(source), msg)
}
