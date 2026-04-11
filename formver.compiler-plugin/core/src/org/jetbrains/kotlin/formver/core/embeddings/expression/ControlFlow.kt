/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.conversion.ReturnTarget
import org.jetbrains.kotlin.formver.core.embeddings.*
import org.jetbrains.kotlin.formver.core.embeddings.callables.FullNamedFunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.callables.NamedFunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.*
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SymbolicName

private data class BlockImpl(override val exps: List<ExpEmbedding>) : Block

fun blockOf(vararg exps: ExpEmbedding): Block = BlockImpl(exps.toList())

fun List<ExpEmbedding>.toBlock(): Block = BlockImpl(this)

fun Block(actions: MutableList<ExpEmbedding>.() -> Unit): Block = BlockImpl(buildList {
    actions()
})

sealed interface Block : ExpEmbedding {
    val exps: List<ExpEmbedding>
    override val type: TypeEmbedding
        get() = exps.lastOrNull()?.type ?: buildType { unit() }

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = BlockNode(exps.map { it.debugTreeView })

    override fun children(): Sequence<ExpEmbedding> = exps.asSequence()
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitBlock(this)
}

data class If(
    val condition: ExpEmbedding,
    val thenBranch: ExpEmbedding,
    val elseBranch: ExpEmbedding,
    override val type: TypeEmbedding
) :
    ExpEmbedding, DefaultDebugTreeViewImplementation {

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(condition, thenBranch, elseBranch)

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(condition, thenBranch, elseBranch)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitIf(this)
}

data class While(
    val condition: ExpEmbedding,
    val body: ExpEmbedding,
    val breakLabelName: SymbolicName,
    val continueLabelName: SymbolicName,
    val invariants: List<ExpEmbedding>,
) : ExpEmbedding, DefaultDebugTreeViewImplementation {
    override val type: TypeEmbedding = buildType { unit() }

    val continueLabel = LabelEmbedding(continueLabelName, invariants)
    val breakLabel = LabelEmbedding(breakLabelName)

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(condition, body)

    context(nameResolver: NameResolver)
    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(
            breakLabel.debugTreeView.withDesignation("break"),
            continueLabel.debugTreeView.withDesignation("continue"),
        )

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(condition, body)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitWhile(this)
}

data class Goto(val target: LabelLink) : ExpEmbedding, DefaultDebugTreeViewImplementation {
    override val type: TypeEmbedding = buildType { nothing() }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf()

    context(nameResolver: NameResolver)
    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(target.debugTreeView)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitGoto(this)
}

// Using this name to avoid clashes with all our other `Label` types.
data class LabelExp(val label: LabelEmbedding) : ExpEmbedding {
    override val type: TypeEmbedding = buildType { unit() }

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode("Label", label.debugTreeView)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitLabelExp(this)
}

/**
 * An expression that optionally has a label and that uses a goto to exit.
 *
 * The result of the intermediate expression is stored.
 */
data class GotoChainNode(val label: LabelEmbedding?, val exp: ExpEmbedding, val next: LabelLink) :
    ExpEmbedding {
    override val type: TypeEmbedding = exp.type

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode("GotoChainNode", listOfNotNull())

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(exp)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitGotoChainNode(this)
}

data class NonDeterministically(val exp: ExpEmbedding) : ExpEmbedding, DefaultDebugTreeViewImplementation {
    override val type: TypeEmbedding = buildType { unit() }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(exp)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitNonDeterministically(this)
}

// Note: this is always a *real* Viper method call.
data class MethodCall(val method: NamedFunctionSignature, val args: List<ExpEmbedding>) : ExpEmbedding {
    override val type: TypeEmbedding = method.callableType.returnType

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode(
            "MethodCall",
            buildList {
                add(method.nameAsDebugTreeView.withDesignation("callee"))
                addAll(args.map { it.debugTreeView })
            })

    override fun children(): Sequence<ExpEmbedding> = args.asSequence()
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitMethodCall(this)
}

data class FunctionCall(val function: NamedFunctionSignature, val args: List<ExpEmbedding>) : ExpEmbedding {
    override val type: TypeEmbedding = function.callableType.returnType

    val subexpressions: List<ExpEmbedding>
        get() = args

    override fun <R> accept(v: ExpVisitor<R>): R =
        v.visitFunctionCall(this)

    override fun children(): Sequence<ExpEmbedding> = args.asSequence()

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode(javaClass.simpleName, args.map { it.debugTreeView })
}

/**
 * We need to generate a fresh variable here since we want to havoc the result.
 *
 * TODO: do this with an explicit havoc in `toViperMaybeStoringIn`.
 */
data class InvokeFunctionObject(
    val receiver: ExpEmbedding,
    val args: List<ExpEmbedding>,
    override val type: TypeEmbedding
) :
    ExpEmbedding {

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode(
            "InvokeFunctionObject",
            buildList {
                add(receiver.debugTreeView.withDesignation("receiver"))
                addAll(args.map { it.debugTreeView })
            })

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitInvokeFunctionObject(this)
}

data class FunctionExp(
    val signature: FullNamedFunctionSignature?,
    val body: ExpEmbedding,
    val returnLabel: LabelEmbedding
) :
    ExpEmbedding {
    override val type: TypeEmbedding = body.type

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode(
            "Function",
            listOfNotNull(
                signature?.nameAsDebugTreeView?.withDesignation("name"),
                body.debugTreeView,
                returnLabel.debugTreeView.withDesignation("return")
            )
        )

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(body)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitFunctionExp(this)
}

data class Elvis(val left: ExpEmbedding, val right: ExpEmbedding, override val type: TypeEmbedding) :
    ExpEmbedding,
    DefaultDebugTreeViewImplementation {
    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(left, right)

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(left, right)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitElvis(this)
}

data class Return(
    val returnExp: ExpEmbedding, val target: ReturnTarget
) : ExpEmbedding {
    override val type = buildType { nothing() }

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode(
            "Return",
            listOf(
                target.variable.debugTreeView,
                returnExp.debugTreeView
            )
        )

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitReturn(this)

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(returnExp)
}
