/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType

/** Reads the `size` domain function for an IntArray, resulting in an Int-typed Ref. */
data class IntArraySize(val arr: ExpEmbedding) : ExpEmbedding {
    override val type: TypeEmbedding = buildType { int() }

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitIntArraySize(this)
    override fun children(): Sequence<ExpEmbedding> = sequenceOf(arr)
}

/**
 * Reads element at [index] from IntArray [arr].
 *
 * Translates to `unfolding acc(IntArray_unique(arr), write) in slot(arr, intFromRef(index)).array_cell_int`.
 * This is a pure expression because `unfolding in` is a pure Viper construct.
 */
data class IntArrayGet(val arr: ExpEmbedding, val index: ExpEmbedding) : ExpEmbedding {
    override val type: TypeEmbedding = buildType { int() }

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitIntArrayGet(this)
    override fun children(): Sequence<ExpEmbedding> = sequenceOf(arr, index)
}

/**
 * Writes [value] to element at [index] in IntArray [arr].
 *
 * Produces: unfold, field write, fold (impure — modifies state).
 */
data class IntArraySet(val arr: ExpEmbedding, val index: ExpEmbedding, val value: ExpEmbedding) : ExpEmbedding {
    override val type: TypeEmbedding = buildType { unit() }

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitIntArraySet(this)
    override fun children(): Sequence<ExpEmbedding> = sequenceOf(arr, index, value)
}


data class IntArrayAsMultiset(val arr: ExpEmbedding) : ExpEmbedding {
    override val type: TypeEmbedding = buildType { any() }

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitIntArrayAsMultiset(this)
    override fun children(): Sequence<ExpEmbedding> = sequenceOf(arr)
}
