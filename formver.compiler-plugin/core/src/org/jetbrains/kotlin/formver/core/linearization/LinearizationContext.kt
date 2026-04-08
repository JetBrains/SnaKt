/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.conversion.ReturnTarget
import org.jetbrains.kotlin.formver.core.embeddings.expression.AnonymousVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.FieldAccess
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.PretypeBuilder
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeBuilder
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.embeddings.types.predicateAccess
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
    val source: KtSourceElement?
    val logicOperatorPolicy: LogicOperatorPolicy

    fun freshAnonVar(type: TypeEmbedding): AnonymousVariableEmbedding

    fun asBlock(action: LinearizationContext.() -> Unit): Stmt.Seqn
    fun <R> withPosition(newSource: KtSourceElement, action: LinearizationContext.() -> R): R

    fun addStatement(buildStmt: LinearizationContext.() -> Stmt)
    fun addDeclaration(decl: Declaration)
    fun store(lhs: VariableEmbedding, rhs: ExpEmbedding)
    fun addReturn(returnExp: ExpEmbedding, target: ReturnTarget)
    fun addBranch(
        condition: ExpEmbedding,
        thenBranch: ExpEmbedding,
        elseBranch: ExpEmbedding,
        type: TypeEmbedding,
        result: VariableEmbedding?
    )

    fun addFieldAccess(access: FieldAccess): Exp

    fun addFieldAccessStoringIn(access: FieldAccess, result: VariableEmbedding)

    /**
     * Unfold [predicates] in the appropriate way for this context, then return [innerViper].
     *
     * When [nullGuard] is non-null, the unfolding is conditional on that expression being true (used for nullable
     * upcasts, where the value may be null).
     *
     * - [Linearizer]: emits `Stmt.Unfold` for each predicate, optionally wrapped in `Stmt.If`
     * - [PureFunBodyLinearizer]: registers predicates on an SSA variable so [SsaConverter]
     *   wraps the *usage* (FuncApp/FieldAccess) with `Exp.Unfolding`, not the argument
     * - [PureExpLinearizer]: throws — correct placement requires SSA, which this linearizer
     *   does not have
     */
    fun applyUnfolding(
        predicates: List<Exp.PredicateAccess>,
        innerViper: Exp,
        innerType: TypeEmbedding,
        nullGuard: Exp? = null,
    ): Exp {
        val unfolded = predicates.foldRight(innerViper) { pred, acc -> Exp.Unfolding(pred, acc) }
        return if (nullGuard != null) Exp.TernaryExp(nullGuard, unfolded, innerViper) else unfolded
    }

    fun addModifier(mod: StmtModifier)

    fun resolveVariableName(name: SymbolicName): SymbolicName
}

fun LinearizationContext.freshAnonVar(init: TypeBuilder.() -> PretypeBuilder): AnonymousVariableEmbedding =
    freshAnonVar(buildType(init))

fun LinearizationContext.addLabel(label: Label) {
    addDeclaration(label.toDecl())
    addStatement { label.toStmt() }
}