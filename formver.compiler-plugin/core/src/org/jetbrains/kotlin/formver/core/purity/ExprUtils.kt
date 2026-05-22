/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.purity

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.diagnostics.ErrorCollectionContext
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.expressionSource
import org.jetbrains.kotlin.formver.core.embeddings.expression.preorder
import org.jetbrains.kotlin.formver.core.exhaustiveAll

/**
 * Validates all nodes using [ValidityVisitor].
 * Avoids `all` to prevent short-circuiting, ensuring all errors are reported.
 */
fun ExpEmbedding.checkValidity(source: KtSourceElement?, errors: ErrorCollectionContext): Boolean =
    preorder(source).exhaustiveAll { (embedding, src) ->
        val nodeSource = checkNotNull(src) {
            "Purity-check expected a KtSourceElement, but none was present"
        }
        val ctx = PurityContext { e, msg ->
            errors.reportPurityViolation(e.expressionSource(nodeSource), msg)
        }
        embedding.accept(ValidityVisitor(ctx))
    }

/**
 * Runs the purity check on a provided embedding
 */
fun ExpEmbedding.isPure(): Boolean = this.accept(ExprPurityVisitor())
