package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType

data class FoldEmbedding(
    val path: PrimitiveFieldAccess,
) : ExpEmbedding {
    override val type: TypeEmbedding
        get() = buildType { boolean() }

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitFoldEmbedding(this)
    override fun children(): Sequence<ExpEmbedding> = sequenceOf(path)
}

data class UnfoldEmbedding(
    val path: PrimitiveFieldAccess,
) : ExpEmbedding {
    override val type: TypeEmbedding
        get() = buildType { boolean() }

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitUnfoldEmbedding(this)
    override fun children(): Sequence<ExpEmbedding> = sequenceOf(path)
}

