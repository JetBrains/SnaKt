/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.conversion.ReturnTarget
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.expression.AnonymousVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.PretypeBuilder
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeBuilder
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Label
import org.jetbrains.kotlin.formver.viper.ast.Stmt

enum class LogicOperatorPolicy {
    CONVERT_TO_IF, CONVERT_TO_EXPRESSION;
}

/**
 * Context in which an `ExpEmbedding` can be flattened to an `Exp` and a sequence of `Stmt`s.
 *
 * We do not distinguish between expressions and statements on the Kotlin side, but we do on the Viper side.
 * As such, an `ExpEmbedding` can represent a nested structure that has to be flattened into sequences
 * of statements. We call this process linearization.
 */
interface LinearizationContext {
    // TODO: Move position tracking out of LinearizationContext and into LinearizationVisitor,
    //  passing Position explicitly to ctx methods that need it.
    val source: KtSourceElement?
    val logicOperatorPolicy: LogicOperatorPolicy

    val typeResolver: TypeResolver

    fun freshAnonVar(type: TypeEmbedding): AnonymousVariableEmbedding

    fun asBlock(action: LinearizationContext.() -> Unit): Stmt.Seqn
    fun <R> withPosition(newSource: KtSourceElement, action: LinearizationContext.() -> R): R

    fun addStatement(buildStmt: LinearizationContext.() -> Stmt)
    fun addDeclaration(decl: Declaration)
    fun store(lhs: VariableEmbedding, rhs: Linearizable)
    fun addReturn(returnExp: Linearizable, target: ReturnTarget)
    fun addBranch(
        condition: Linearizable,
        thenBranch: Linearizable,
        elseBranch: Linearizable,
        result: VariableEmbedding?
    )

    fun addFieldAccess(
        receiver: Linearizable,
        receiverType: TypeEmbedding,
        field: FieldEmbedding,
        receiverIsUnique: Boolean = false,
    ): Exp

    fun addFieldAccessStoringIn(
        receiver: Linearizable,
        receiverType: TypeEmbedding,
        field: FieldEmbedding,
        result: VariableEmbedding,
        receiverIsUnique: Boolean = false,
    )

    fun addModifier(mod: StmtModifier)

    fun resolveVariableName(name: SymbolicName): SymbolicName

    /**
     * Register that [receiverName] is an `@Unique` parameter eligible for function-scope
     * unfolding. The actual `Stmt.Unfold` is *not* emitted here; it's deferred until the
     * first access via [tryUseFunctionScopedReceiver], so functions that never touch the
     * parameter don't emit a no-op unfold/fold pair.
     *
     * Default: no-op (only the main body Linearizer uses this; pure-expression linearizers
     * don't need it since they never emit unfold/fold).
     */
    fun registerEligibleReceiver(receiverName: SymbolicName, predAcc: Exp.PredicateAccess) {}

    /**
     * If [receiverName] is registered (and the receiver is therefore at function-scope
     * unfolded by convention), return true and emit the deferred `Stmt.Unfold` if this is
     * the first access. The caller then emits the field op directly without its own
     * unfold/fold pair. Returns false for receivers that aren't eligible — the caller
     * falls back to the per-access cycle (or the conservative havoc).
     *
     * Default: false.
     */
    fun tryUseFunctionScopedReceiver(receiverName: SymbolicName): Boolean = false

    /**
     * Emit a `fold` for every receiver that was actually unfolded by
     * [tryUseFunctionScopedReceiver]. Used at function exit (epilogue and each early-return
     * goto). Receivers registered but never accessed are skipped. Default: no-op.
     */
    fun foldUsedFunctionScopedReceivers() {}
}

fun LinearizationContext.freshAnonVar(init: TypeBuilder.() -> PretypeBuilder): AnonymousVariableEmbedding =
    freshAnonVar(buildType(init))

fun LinearizationContext.addLabel(label: Label) {
    addDeclaration(label.toDecl())
    addStatement { label.toStmt() }
}
