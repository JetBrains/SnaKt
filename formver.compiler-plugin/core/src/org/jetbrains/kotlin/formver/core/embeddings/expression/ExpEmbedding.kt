/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.SourceRole
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.*
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.core.linearization.linearize
import org.jetbrains.kotlin.formver.core.linearization.pureToViper
import org.jetbrains.kotlin.formver.core.purity.PurityContext
import org.jetbrains.kotlin.formver.names.SimpleNameResolver
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.formver.viper.debugMangled
import org.jetbrains.kotlin.formver.viper.mangled

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
}

sealed class ToViperBuiltinMisuseError(msg: String) : RuntimeException(msg)

class ToViperBuiltinOnlyError(exp: ExpEmbedding, nameResolver: NameResolver = SimpleNameResolver()) :
    ToViperBuiltinMisuseError(with(nameResolver) { "${exp.debugTreeView.print()} can only be translated to Viper built-in type" })

/**
 * Default `debugTreeView` implementation that collects trees from a number of possible formats.
 *
 * This covers most use-cases.
 * We don't give `debugAnonymousSubexpressions` a default value since not specifying it explicitly is a good sign we just forgot
 * to implement things for that class.
 */
sealed interface DefaultDebugTreeViewImplementation : ExpEmbedding {
    val debugName: String
        get() = javaClass.simpleName
    val debugAnonymousSubexpressions: List<ExpEmbedding>
    val debugNamedSubexpressions: Map<String, ExpEmbedding>
        get() = mapOf()

    context(nameResolver: NameResolver)
    val debugExtraSubtrees: List<TreeView>
        get() = listOf()

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() {
            val anonymousSubtrees = debugAnonymousSubexpressions.map { it.debugTreeView }
            val namedSubtrees =
                debugNamedSubexpressions.map {
                    designatedNode(
                        it.key,
                        it.value.debugTreeView
                    )
                }
            val allSubtrees = anonymousSubtrees + namedSubtrees + debugExtraSubtrees
            return if (allSubtrees.isNotEmpty()) NamedBranchingNode(debugName, allSubtrees)
            else PlaintextLeaf(debugName)
        }
}

data class PrimitiveFieldAccess(val inner: ExpEmbedding, val field: FieldEmbedding) :
    ExpEmbedding {
    override val type: TypeEmbedding
        get() = this.field.type

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = OperatorNode(inner.debugTreeView, ".", this.field.debugTreeView)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitPrimitiveFieldAccess(this)
    override fun children(): Sequence<ExpEmbedding> = sequenceOf(inner)
}

data class FieldAccess(val receiver: ExpEmbedding, val field: FieldEmbedding) : ExpEmbedding {
    override val type: TypeEmbedding = field.type

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = OperatorNode(receiver.debugTreeView, ".", this.field.debugTreeView)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitFieldAccess(this)
    override fun children(): Sequence<ExpEmbedding> = sequenceOf(receiver)
}

/**
 * Represents a combination of `Assign` + `FieldAccess`.
 */
data class FieldModification(val receiver: ExpEmbedding, val field: FieldEmbedding, val newValue: ExpEmbedding) :
    ExpEmbedding {
    override val type: TypeEmbedding = buildType { unit() }

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = OperatorNode(
            OperatorNode(receiver.debugTreeView, ".", this.field.debugTreeView),
            " := ",
            newValue.debugTreeView
        )

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitFieldModification(this)
}

data class FieldAccessPermissions(val inner: ExpEmbedding, val field: FieldEmbedding, val perm: PermExp) :
    ExpEmbedding {
    override val type: TypeEmbedding = buildType { boolean() }

    // field collides with the field context-sensitive keyword.
    context(nameResolver: NameResolver)
    val debugExtraSubtrees: List<TreeView>
        get() = listOf(this.field.debugTreeView, perm.debugTreeView)

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() {
            val allSubtrees = debugExtraSubtrees
            return if (allSubtrees.isNotEmpty()) NamedBranchingNode(javaClass.simpleName, allSubtrees)
            else PlaintextLeaf(javaClass.simpleName)
        }

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

    val subexpressions: List<ExpEmbedding>
        get() = args

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode("PredicateAccess", buildList {
            add(PlaintextLeaf(predicateName.mangled).withDesignation("name"))
            addAll(args.map { it.debugTreeView })
        })

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitPredicateAccessPermissions(this)
    override fun children(): Sequence<ExpEmbedding> = args.asSequence()
}

data class Assign(val lhs: VariableEmbedding, val rhs: ExpEmbedding) : ExpEmbedding {
    override val type: TypeEmbedding = lhs.type

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = OperatorNode(lhs.debugTreeView, " := ", rhs.debugTreeView)

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(lhs, rhs)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitAssign(this)
}

data class Declare(val variable: VariableEmbedding, val initializer: ExpEmbedding?) : ExpEmbedding,
    DefaultDebugTreeViewImplementation {
    override val type: TypeEmbedding = buildType { unit() }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf()

    context(nameResolver: NameResolver)
    override val debugExtraSubtrees: List<TreeView>
        get() = listOfNotNull(variable.debugTreeView, variable.type.debugTreeView, initializer?.debugTreeView)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitDeclare(this)
}
