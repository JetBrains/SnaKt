/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.SourceRole
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.*
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.purity.PurityContext
import org.jetbrains.kotlin.formver.names.SimpleNameResolver
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.PermExp

sealed interface ExpEmbedding : DebugPrintable {
    val type: TypeEmbedding

    /**
     * The original Kotlin source's role for the generated expression embedding.
     */
    val sourceRole: SourceRole?
        get() = null

    fun ignoringCasts(): ExpEmbedding = this

    /**
     * Meta nodes are nodes like `WithPosition`.
     */
    fun ignoringMetaNodes(): ExpEmbedding = this

    fun ignoringCastsAndMetaNodes(): ExpEmbedding = this

    fun children(): Sequence<ExpEmbedding> = emptySequence()
    fun <R> accept(v: ExpVisitor<R>): R
    fun isValid(ctx: PurityContext): Boolean = true

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = accept(DebugTreeViewVisitor(nameResolver))
}

sealed class ToViperBuiltinMisuseError(msg: String) : RuntimeException(msg)

class ToViperBuiltinOnlyError(exp: ExpEmbedding, nameResolver: NameResolver = SimpleNameResolver()) :
    ToViperBuiltinMisuseError(with(nameResolver) { "${exp.debugTreeView.print()} can only be translated to Viper built-in type" })

data class PrimitiveFieldAccess(val inner: ExpEmbedding, val field: FieldEmbedding) :
    ExpEmbedding {
    override val type: TypeEmbedding
        get() = this.field.type

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitPrimitiveFieldAccess(this)
    override fun children(): Sequence<ExpEmbedding> = sequenceOf(inner)
}

data class FieldAccess(
    val receiver: ExpEmbedding,
    val field: FieldEmbedding,
    val receiverIsUnique: Boolean = false,
) : ExpEmbedding {
    override val type: TypeEmbedding = field.type

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitFieldAccess(this)
    override fun children(): Sequence<ExpEmbedding> = sequenceOf(receiver)
}

/**
 * Represents a combination of `Assign` + `FieldAccess`.
 */
data class FieldModification(
    val receiver: ExpEmbedding,
    val field: FieldEmbedding,
    val newValue: ExpEmbedding,
    val receiverIsUnique: Boolean = false,
) : ExpEmbedding {
    override val type: TypeEmbedding = buildType { unit() }

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(receiver, newValue)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitFieldModification(this)
}

data class FieldAccessPermissions(val inner: ExpEmbedding, val field: FieldEmbedding, val perm: PermExp) :
    ExpEmbedding {
    override val type: TypeEmbedding = buildType { boolean() }

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitFieldAccessPermissions(this)
    override fun children(): Sequence<ExpEmbedding> = sequenceOf(inner)
}

// Ideally we would use the predicate, but due to the possibility of recursion this is inconvenient at present.
data class PredicateAccessPermissions(
    val predicateName: SymbolicName,
    val args: List<ExpEmbedding>,
    val perm: PermExp
) :
    ExpEmbedding {
    override val type: TypeEmbedding = buildType { boolean() }

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitPredicateAccessPermissions(this)
    override fun children(): Sequence<ExpEmbedding> = args.asSequence()
}

data class Assign(val lhs: VariableEmbedding, val rhs: ExpEmbedding) : ExpEmbedding {
    override val type: TypeEmbedding = lhs.type

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(lhs, rhs)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitAssign(this)
}

data class Declare(val variable: VariableEmbedding, val initializer: ExpEmbedding?) : ExpEmbedding {
    override val type: TypeEmbedding = buildType { unit() }

    override fun children(): Sequence<ExpEmbedding> = listOfNotNull(variable, initializer).asSequence()
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitDeclare(this)
}
