/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.conversion.ReturnTarget
import org.jetbrains.kotlin.formver.core.embeddings.*
import org.jetbrains.kotlin.formver.core.embeddings.callables.FullNamedFunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.callables.NamedFunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.callables.toFuncApp
import org.jetbrains.kotlin.formver.core.embeddings.callables.toMethodCall
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.*
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.core.linearization.addLabel
import org.jetbrains.kotlin.formver.core.linearization.freshAnonVar
import org.jetbrains.kotlin.formver.core.linearization.pureToViper
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt

private data class BlockImpl(override val exps: List<ExpEmbedding>) : Block

/** Creates a [Block] from a vararg list of expressions. */
fun blockOf(vararg exps: ExpEmbedding): Block = BlockImpl(exps.toList())

/** Converts a list of expressions into a [Block]. */
fun List<ExpEmbedding>.toBlock(): Block = BlockImpl(this)

/** Builds a [Block] using a list-builder lambda. */
fun Block(actions: MutableList<ExpEmbedding>.() -> Unit): Block = BlockImpl(buildList {
    actions()
})

/**
 * A sequential block of expressions evaluated for side effects, with the last expression
 * providing the result and type. An empty block has type `Unit`.
 */
sealed interface Block : OptionalResultExpEmbedding {
    val exps: List<ExpEmbedding>
    override val type: TypeEmbedding
        get() = exps.lastOrNull()?.type ?: buildType { unit() }

    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        if (exps.isEmpty()) return

        for (exp in exps.take(exps.size - 1)) {
            exp.toViperUnusedResult(ctx)
        }
        exps.last().toViperMaybeStoringIn(result, ctx)
    }

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = BlockNode(exps.map { it.debugTreeView })

    override fun children(): Sequence<ExpEmbedding> = exps.asSequence()
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitBlock(this)
}

/**
 * A conditional expression with a boolean [condition] and two branches.
 * The [type] must be supplied explicitly as the common type of both branches.
 * Lowered to a Viper `if`/`else` statement via [LinearizationContext.addBranch].
 */
data class If(
    val condition: ExpEmbedding,
    val thenBranch: ExpEmbedding,
    val elseBranch: ExpEmbedding,
    override val type: TypeEmbedding
) :
    OptionalResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        ctx.addBranch(condition, thenBranch, elseBranch, type, result)
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(condition, thenBranch, elseBranch)

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(condition, thenBranch, elseBranch)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitIf(this)
}

/**
 * A while-loop embedding.
 *
 * Lowered to a Viper `if` guarded by [condition] with a body that jumps back to the
 * [continueLabel] (the loop head). The [breakLabel] marks the exit point after the loop.
 * [invariants] are asserted via `assert` statements after the loop body (pending a Viper
 * version upgrade that supports native loop invariants — see TODO in [toViperSideEffects]).
 */
data class While(
    val condition: ExpEmbedding,
    val body: ExpEmbedding,
    val breakLabelName: SymbolicName,
    val continueLabelName: SymbolicName,
    val invariants: List<ExpEmbedding>,
) : UnitResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override val type: TypeEmbedding = buildType { unit() }

    val continueLabel = LabelEmbedding(continueLabelName, invariants)
    val breakLabel = LabelEmbedding(breakLabelName)

    override fun toViperSideEffects(ctx: LinearizationContext) {
        ctx.addLabel(continueLabel.toViper(ctx))
        val condVar = ctx.freshAnonVar { boolean() }
        condition.toViperStoringIn(condVar, ctx)
        ctx.addStatement {
            val bodyBlock = ctx.asBlock {
                body.toViperUnusedResult(this)
                addStatement { continueLabel.toLink().toViperGoto(this) }
            }
            Stmt.If(condVar.toViperBuiltinType(ctx), bodyBlock, els = Stmt.Seqn(), ctx.source.asPosition)
        }
        ctx.addLabel(breakLabel.toViper(ctx))

        // TODO: this logic can be rewritten back to invariants once the version of Viper is updated
        invariants.forEach {
            ctx.addStatement {
                Stmt.Assert(it.pureToViper(toBuiltin = true))
            }
        }
    }

    // TODO: add invariants
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

/** An unconditional jump to [target]. Has type `Nothing` since control never continues past it. */
data class Goto(val target: LabelLink) : NoResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override val type: TypeEmbedding = buildType { nothing() }
    override fun toViperUnusedResult(ctx: LinearizationContext) {
        ctx.addStatement { target.toViperGoto(ctx) }
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf()

    context(nameResolver: NameResolver)
    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(target.debugTreeView)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitGoto(this)
}

/**
 * An expression that emits a Viper label statement at the current position.
 * Named `LabelExp` to avoid clashing with other `Label`-named types in the codebase.
 */
data class LabelExp(val label: LabelEmbedding) : UnitResultExpEmbedding {
    override fun toViperSideEffects(ctx: LinearizationContext) {
        ctx.addLabel(label.toViper(ctx))
    }

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode("Label", label.debugTreeView)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitLabelExp(this)
}

/**
 * A node in a goto-chain: optionally emits [label], evaluates [exp] storing its result, then
 * unconditionally jumps to [next]. Used to link SSA blocks in sequence.
 */
data class GotoChainNode(val label: LabelEmbedding?, val exp: ExpEmbedding, val next: LabelLink) :
    OptionalResultExpEmbedding {
    override val type: TypeEmbedding = exp.type

    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        label?.let { ctx.addLabel(it.toViper(ctx)) }
        ctx.addStatement {
            exp.toViperMaybeStoringIn(result, ctx)
            next.toViperGoto(ctx)
        }
    }

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = NamedBranchingNode("GotoChainNode", listOfNotNull())

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(exp)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitGotoChainNode(this)
}

/**
 * Wraps [exp] in a non-deterministic branch: lowered to a Viper `if (*)` so the expression
 * may or may not execute. Used to model optional side effects without committing to a path.
 */
data class NonDeterministically(val exp: ExpEmbedding) : UnitResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override fun toViperSideEffects(ctx: LinearizationContext) {
        ctx.addStatement {
            val choice = ctx.freshAnonVar { boolean() }
            val expViper = ctx.asBlock { exp.toViper(this) }
            Stmt.If(choice.toViperBuiltinType(ctx), expViper, Stmt.Seqn(), ctx.source.asPosition)
        }
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(exp)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitNonDeterministically(this)
}

/**
 * A call to a Viper `method` (not a Viper `function`).
 *
 * Always generates a Viper method-call statement via [NamedFunctionSignature.toMethodCall].
 * The result is written into a fresh variable supplied by [StoredResultExpEmbedding].
 * Contrast with [FunctionCall], which emits a pure Viper function-application expression.
 */
data class MethodCall(val method: NamedFunctionSignature, val args: List<ExpEmbedding>) : StoredResultExpEmbedding {
    override val type: TypeEmbedding = method.callableType.returnType

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        ctx.addStatement {
            method.toMethodCall(
                args.map { it.toViper(ctx) },
                result.toLocalVarUse(ctx.source.asPosition),
                ctx.source.asPosition
            )
        }
    }

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

/**
 * A call to a Viper `function` (pure, expression-level).
 *
 * Emits a Viper function-application expression via [NamedFunctionSignature.toFuncApp].
 * Contrast with [MethodCall], which emits a statement-level Viper method call.
 */
data class FunctionCall(val function: NamedFunctionSignature, val args: List<ExpEmbedding>) : DirectResultExpEmbedding {
    override val type: TypeEmbedding = function.callableType.returnType

    override val subexpressions: List<ExpEmbedding>
        get() = args

    override fun toViper(ctx: LinearizationContext): Exp = function.toFuncApp(
        args.map { it.toViper(ctx) },
        ctx.source.asPosition
    )

    override fun <R> accept(v: ExpVisitor<R>): R =
        v.visitFunctionCall(this)
}

/**
 * Represents a call through a first-class function object (e.g. a lambda or function reference).
 *
 * Because the exact callee is unknown statically, the result is havoc'd: a fresh anonymous
 * variable of [type] is created and its invariants are inhaled. The receiver and arguments
 * are evaluated for side effects but their values are discarded.
 *
 * TODO: do this with an explicit havoc in `toViperMaybeStoringIn`.
 */
data class InvokeFunctionObject(
    val receiver: ExpEmbedding,
    val args: List<ExpEmbedding>,
    override val type: TypeEmbedding
) :
    OnlyToViperExpEmbedding {
    override fun toViper(ctx: LinearizationContext): Exp {
        val variable = ctx.freshAnonVar(type)
        receiver.toViperUnusedResult(ctx)
        for (arg in args) arg.toViperUnusedResult(ctx)
        // TODO: figure out which exactly invariants we want here
        return variable.withInvariants {
            proven = true
            access = true
        }.toViper(ctx)
    }

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

/**
 * The top-level wrapper for a non-pure (Viper method) function body.
 *
 * On linearization:
 * 1. For each formal parameter, inhales its `provenInvariants` (runtime type) and
 *    `sharedPredicateAccessInvariant` (heap permissions) so the body can rely on them.
 *    Note: `@Pure` functions instead receive these as `requires` preconditions, since
 *    pure Viper functions cannot contain statements.
 * 2. Linearizes [body].
 * 3. Emits [returnLabel] so that `return` expressions inside [body] can jump here.
 *
 * [signature] is nullable to support anonymous/lambda bodies that have no named signature.
 */
data class FunctionExp(
    val signature: FullNamedFunctionSignature?,
    val body: ExpEmbedding,
    val returnLabel: LabelEmbedding
) :
    OptionalResultExpEmbedding {
    override val type: TypeEmbedding = body.type

    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        signature?.formalArgs?.forEach { arg ->
            // Ideally we would want to assume these rather than inhale them to prevent inconsistencies with permissions.
            // Unfortunately Silicon for some reason does not allow Assumes. However, it doesn't matter as long as the
            // provenInvariants don't contain permissions.
            // TODO (inhale vs require) Decide if `predicateAccessInvariant` should be required rather than inhaled in the beginning of the body.
            (arg.provenInvariants() + listOfNotNull(arg.sharedPredicateAccessInvariant())).forEach { invariant ->
                ctx.addStatement { Stmt.Inhale(invariant.toViperBuiltinType(ctx), ctx.source.asPosition) }
            }
        }
        body.toViperMaybeStoringIn(result, ctx)
        ctx.addLabel(returnLabel.toViper(ctx))
    }

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

/**
 * The Elvis operator (`left ?: right`).
 *
 * If [left] is non-null it is used as the result; otherwise [right] is evaluated and used.
 * Lowered to an [If] conditioned on a null-check of [left].
 */
data class Elvis(val left: ExpEmbedding, val right: ExpEmbedding, override val type: TypeEmbedding) :
    StoredResultExpEmbedding,
    DefaultDebugTreeViewImplementation {
    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        val leftViper = left.toViper(ctx)
        val leftWrapped = ExpWrapper(leftViper, left.type)
        val conditional = If(leftWrapped.notNullCmp(), leftWrapped, right, type)
        conditional.toViperStoringIn(result, ctx)
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(left, right)

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(left, right)
    override fun <R> accept(v: ExpVisitor<R>): R = v.visitElvis(this)
}

/**
 * A `return` expression that transfers [returnExp] to [target] and jumps to the target's label.
 *
 * Has type `Nothing` because control never falls through. Used for both plain `return` and
 * labelled `return@label` expressions; the correct [ReturnTarget] is resolved during conversion.
 */
data class Return(
    val returnExp: ExpEmbedding, val target: ReturnTarget
) : OptionalResultExpEmbedding {
    override val type = buildType { nothing() }

    override fun toViperMaybeStoringIn(result: VariableEmbedding?, ctx: LinearizationContext) {
        ctx.addReturn(returnExp, target)
    }

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