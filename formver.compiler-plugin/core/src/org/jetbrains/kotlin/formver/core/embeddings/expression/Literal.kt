/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.SourceRole
import org.jetbrains.kotlin.formver.core.embeddings.types.AdtConstructorEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.AdtTypeEmbedding
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
 * Embeds a reference to a Kotlin object which was declared to be an ADT.
 *
 * Holds the [AdtConstructorEmbedding] and delegates Viper code generation to the linearization visitor.
 */
data class AdtConstructorLit(
    override val type: TypeEmbedding,
    val constructorEmbedding: AdtConstructorEmbedding,
    val adtTypeEmbedding: AdtTypeEmbedding,
) : ExpEmbedding {
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitAdtConstructorLit(this)
}

