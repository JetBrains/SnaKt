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
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.withType
import org.jetbrains.kotlin.formver.core.embeddings.toLink
import org.jetbrains.kotlin.formver.core.embeddings.toViperGoto
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Position
import org.jetbrains.kotlin.formver.viper.ast.Stmt

/**
 * Standard linearization context: converts an [ExpEmbedding] tree into a flat Viper statement
 * sequence ([Stmt.Seqn]).
 *
 * Each function body is linearized by creating a [Linearizer] rooted at a fresh [SeqnBuilder].
 * As the embedding tree is traversed, statements are accumulated via [addStatement] and
 * [addDeclaration]; nested blocks (e.g., `if`-branches, `while`-bodies) use [asBlock] to produce
 * child [Stmt.Seqn] nodes.
 *
 * After linearization, the resulting [Stmt.Seqn] is passed to [SsaConverter] for SSA renaming.
 *
 * @property state Shared state for the whole function body: fresh-name source and label targets.
 * @property seqnBuilder Accumulates statements for the current block scope.
 * @property source The Kotlin source element used as the default position for emitted statements.
 * @property stmtModifierTracker Tracks [StmtModifier]s around the next [addStatement] call; `null` outside a statement context.
 */
data class Linearizer(
    val state: SharedLinearizationState,
    val seqnBuilder: SeqnBuilder,
    override val source: KtSourceElement?,
    val stmtModifierTracker: StmtModifierTracker? = null
) : LinearizationContext {
    override val unfoldPolicy: UnfoldPolicy
        get() = UnfoldPolicy.UNFOLD
    override val logicOperatorPolicy: LogicOperatorPolicy
        get() = LogicOperatorPolicy.CONVERT_TO_IF

    /** Creates a fresh anonymous variable of the given [type] and registers it as a local declaration in the current block. */
    override fun freshAnonVar(type: TypeEmbedding): AnonymousVariableEmbedding {
        val variable = state.freshAnonVar(type)
        addDeclaration(variable.toLocalVarDecl())
        return variable
    }

    /**
     * Executes [action] in a fresh block scope and returns the resulting [Stmt.Seqn].
     * Used to build the then/else branches of `if`-statements and the body of `while`-loops.
     */
    override fun asBlock(action: LinearizationContext.() -> Unit): Stmt.Seqn {
        val newBuilder = SeqnBuilder(source)
        copy(seqnBuilder = newBuilder).action()
        return newBuilder.block
    }

    /** Returns a copy of this context with [source] updated to [newSource], then executes [action]. */
    override fun <R> withPosition(newSource: KtSourceElement, action: LinearizationContext.() -> R): R =
        copy(source = newSource).action()

    /**
     * Builds a Viper statement using [buildStmt], applies any registered [StmtModifier]s around it,
     * and appends the result to the current [SeqnBuilder].
     */
    override fun addStatement(buildStmt: LinearizationContext.() -> Stmt) {
        val addStatementContext = object : AddStatementContext {
            override val position: Position = source.asPosition
            override fun addImmediateStatement(statement: Stmt) {
                seqnBuilder.addStatement(statement)
            }
        }
        val newTracker = StmtModifierTracker()
        val stmt = copy(stmtModifierTracker = newTracker).buildStmt()
        newTracker.applyOnEntry(addStatementContext)
        seqnBuilder.addStatement(stmt)
        newTracker.applyOnExit(addStatementContext)
    }

    /** Adds a local variable declaration to the current block's declaration list. */
    override fun addDeclaration(decl: Declaration) {
        seqnBuilder.addDeclaration(decl)
    }

    /** Emits a Viper assignment of [rhs] (coerced to [lhs]'s type) into [lhs]. */
    override fun store(lhs: VariableEmbedding, rhs: ExpEmbedding) {
        val lhsViper = lhs.toViper(this)
        val rhsViper = rhs.withType(lhs.type).toViper(this)
        addStatement { Stmt.assign(lhsViper, rhsViper, source.asPosition) }
    }

    /**
     * Emits the statements needed to return [returnExp] from the current function:
     * stores the value into [target]'s variable and jumps to [target]'s return label.
     */
    override fun addReturn(returnExp: ExpEmbedding, target: ReturnTarget) {
        returnExp.withType(target.variable.type)
            .toViperStoringIn(target.variable, this)
        addStatement { target.label.toLink().toViperGoto(this) }
    }

    /**
     * Emits a Viper `if`-statement for a conditional expression.
     * Both branches are linearized into child [Stmt.Seqn] nodes via [asBlock].
     * If [result] is non-null, both branches store their result into it.
     */
    override fun addBranch(
        condition: ExpEmbedding,
        thenBranch: ExpEmbedding,
        elseBranch: ExpEmbedding,
        type: TypeEmbedding,
        result: VariableEmbedding?
    ) =
        addStatement {
            val condViper = condition.toViperBuiltinType(this)
            val thenViper = asBlock { thenBranch.withType(type).toViperMaybeStoringIn(result, this) }
            val elseViper = asBlock { elseBranch.withType(type).toViperMaybeStoringIn(result, this) }
            Stmt.If(condViper, thenViper, elseViper, source.asPosition)
        }

    /**
     * Registers a [StmtModifier] that will be applied around the next [addStatement] call.
     * Must only be called from within [addStatement]'s [buildStmt] lambda.
     */
    override fun addModifier(mod: StmtModifier) {
        stmtModifierTracker?.add(mod) ?: error("Not in a statement")
    }

    /** Returns [name] unchanged; SSA renaming is performed later by [SsaConverter]. */
    override fun resolveVariableName(name: SymbolicName): SymbolicName = name
}