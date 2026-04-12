/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding

data class Old(val inner: ExpEmbedding) : ExpEmbedding {
    override val type: TypeEmbedding = inner.type

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitOld(this)
    override fun children(): Sequence<ExpEmbedding> = sequenceOf(inner)
}
