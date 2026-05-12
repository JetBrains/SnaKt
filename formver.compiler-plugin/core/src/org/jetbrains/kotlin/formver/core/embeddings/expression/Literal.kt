/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.SourceRole
import org.jetbrains.kotlin.formver.core.embeddings.types.AdtTypeEmbeddingImpl
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType

interface LiteralEmbedding : ExpEmbedding {
    val value: Any?

    val debugName: String
        get() = javaClass.simpleName

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitLiteralEmbedding(this)
}

data object UnitLit : LiteralEmbedding {
    override val type = buildType { unit() }
    override val value = Unit

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitUnitLit(this)
}

data class IntLit(override val value: Int) : LiteralEmbedding {
    override val type = buildType { int() }
    override val debugName = "Int"
}

data class BooleanLit(
    override val value: Boolean,
    override val sourceRole: SourceRole? = null
) : LiteralEmbedding {
    override val type = buildType { boolean() }
    override val debugName = "Boolean"
}

data class CharLit(
    override val value: Char,
) : LiteralEmbedding {
    override val type = buildType { char() }
    override val debugName: String = "Char"
}

data class StringLit(
    override val value: String,
) : LiteralEmbedding {
    override val type = buildType { string() }
    override val debugName: String = "String"
}

data object NullLit : LiteralEmbedding {
    override val value = null
    override val type = buildType { isNullable = true; nothing() }
    override val debugName = "Null"
}

/**
 * Reference to a literal/constructor of an object/class which was declared to be an ADT in the Kotlin source.
 * As ADTs are value-based, this is treated as an ADT instantiation and triggers an ADT constructor associated
 * with the source object/class.
 */
data class AdtConstructorRef(
    override val type: TypeEmbedding,
    val args: List<ExpEmbedding>,
) : ExpEmbedding {
    init {
        require(type.pretype is AdtTypeEmbeddingImpl) {
            "AdtConstructorRef requires an AdtTypeEmbedding pretype, got ${type.pretype.javaClass.simpleName}"
        }
    }

    val adtTypeEmbedding: AdtTypeEmbeddingImpl
        get() = type.pretype as AdtTypeEmbeddingImpl

    override fun children(): Sequence<ExpEmbedding> = args.asSequence()
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitAdtConstructorRef(this)
}
