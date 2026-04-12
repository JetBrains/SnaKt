/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.domains.InjectionImageFunction
import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.SourceRole
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Exp

interface InjectionBasedExpEmbedding : ExpEmbedding {
    val refsOperation: InjectionImageFunction
    val builtinsOperation
        get() = refsOperation.original
}

interface BinaryOperatorExpEmbedding : InjectionBasedExpEmbedding {
    val left: ExpEmbedding
    val right: ExpEmbedding

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitBinaryOperatorExpEmbedding(this)

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(left, right)
}

interface UnaryOperatorExpEmbedding : InjectionBasedExpEmbedding {
    val inner: ExpEmbedding

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitUnaryOperatorExpEmbedding(this)

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(inner)
}

sealed interface OperatorExpEmbeddingTemplate {
    val type: TypeEmbedding
    val refsOperation: InjectionImageFunction

    companion object {
        fun create(
            type: TypeEmbedding,
            refsOperation: InjectionImageFunction,
        ): OperatorExpEmbeddingTemplate? = when (refsOperation.formalArgs.size) {
            1 -> UnaryOperatorExpEmbeddingTemplate(type, refsOperation)
            2 -> BinaryOperatorExpEmbeddingTemplate(type, refsOperation)
            else -> null
        }
    }
}

class BinaryOperatorExpEmbeddingTemplate(
    override val type: TypeEmbedding,
    override val refsOperation: InjectionImageFunction
) :
    OperatorExpEmbeddingTemplate {
    operator fun invoke(
        left: ExpEmbedding,
        right: ExpEmbedding,
        sourceRole: SourceRole? = null
    ): BinaryOperatorExpEmbedding =
        object : BinaryOperatorExpEmbedding {
            override val refsOperation: InjectionImageFunction = this@BinaryOperatorExpEmbeddingTemplate.refsOperation
            override val type = this@BinaryOperatorExpEmbeddingTemplate.type
            override val left = left
            override val right = right
            override val sourceRole = sourceRole
        }
}

class UnaryOperatorExpEmbeddingTemplate(
    override val type: TypeEmbedding,
    override val refsOperation: InjectionImageFunction
) :
    OperatorExpEmbeddingTemplate {
    operator fun invoke(inner: ExpEmbedding, sourceRole: SourceRole? = null): UnaryOperatorExpEmbedding =
        object : UnaryOperatorExpEmbedding {
            override val refsOperation: InjectionImageFunction = this@UnaryOperatorExpEmbeddingTemplate.refsOperation
            override val type = this@UnaryOperatorExpEmbeddingTemplate.type
            override val inner = inner
            override val sourceRole = sourceRole
        }
}

fun List<ExpEmbedding>.toConjunction(): ExpEmbedding =
    if (isEmpty()) BooleanLit(true)
    else reduce { l, r -> OperatorExpEmbeddings.And(l, r) }
