/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType

data class IntArrayGet(val array: ExpEmbedding, val index: ExpEmbedding) : ExpEmbedding {
    override val type: TypeEmbedding = buildType { int() }

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(array, index)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitIntArrayGet(this)
}

data class IntArraySize(val array: ExpEmbedding) : ExpEmbedding {
    override val type: TypeEmbedding = buildType { int() }

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(array)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitIntArraySize(this)
}

data class IntArraySet(val array: ExpEmbedding, val index: ExpEmbedding, val value: ExpEmbedding) : ExpEmbedding {
    override val type: TypeEmbedding = buildType { unit() }

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(array, index, value)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitIntArraySet(this)
}
