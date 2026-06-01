/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.purity

import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.expression.Assert
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding

internal class ValidityVisitor(private val ctx: PurityContext) : ExpVisitor<Boolean> {
    override fun visitDefault(e: ExpEmbedding): Boolean = true

    override fun visitAssert(e: Assert): Boolean = e.exp.isPure().also {
        if (!it) ctx.addPurityError(e.exp, "Assert condition is impure")
    }
}
