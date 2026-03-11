/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.core.conversion.ReturnTarget
import org.jetbrains.kotlin.formver.core.embeddings.expression.AnonymousVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpWrapper
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.withType
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.toLink
import org.jetbrains.kotlin.formver.core.embeddings.toViperGoto
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Position
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.formver.viper.debugMangled
import kotlin.sequences.forEach

/**
 * Standard context for linearization.
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

    override fun freshAnonVar(type: TypeEmbedding): AnonymousVariableEmbedding {
        val variable = state.freshAnonVar(type)
        addDeclaration(variable.toLocalVarDecl())
        return variable
    }

    override fun asBlock(action: LinearizationContext.() -> Unit): Stmt.Seqn {
        val newBuilder = SeqnBuilder(source)
        copy(seqnBuilder = newBuilder).action()
        return newBuilder.block
    }

    override fun <R> withPosition(newSource: KtSourceElement, action: LinearizationContext.() -> R): R =
        copy(source = newSource).action()

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

    override fun addDeclaration(decl: Declaration) {
        seqnBuilder.addDeclaration(decl)
    }

    override fun store(lhs: VariableEmbedding, rhs: ExpEmbedding) {
        val lhsViper = lhs.toViper(this)
        val rhsViper = rhs.withType(lhs.type).toViper(this)
        addStatement { Stmt.assign(lhsViper, rhsViper, source.asPosition) }
    }

    override fun addReturn(returnExp: ExpEmbedding, target: ReturnTarget) {
        returnExp.withType(target.variable.type)
            .toViperStoringIn(target.variable, this)
        addStatement { target.label.toLink().toViperGoto(this) }
    }

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

    override fun addModifier(mod: StmtModifier) {
        stmtModifierTracker?.add(mod) ?: error("Not in a statement")
    }

    override fun handleFieldAccess(field: FieldEmbedding, receiver: ExpEmbedding, result: VariableEmbedding) {
        addStatement {
            when (field.accessPolicy) {
                // TODO: Handling a unique field on a shared receiver must be added here.
                AccessPolicy.BY_RECEIVER_UNIQUENESS -> {
                    receiver.toViperUnusedResult(this)
                    field.type.havocMethod.toMethodCall(emptyList(), listOf(result.toLocalVarUse()))
                }

                else -> {
                    val receiverViper = receiver.toViper(this)
                    // If the field access is not replaced with havoc,
                    // we might need to unfold some predicate to access it.
                    if (field.unfoldToAccess) {
                        val receiverWrapper = ExpWrapper(receiverViper, receiver.type)
                        val hierarchyPath = (receiver.type.pretype as? ClassTypeEmbedding)?.details?.hierarchyUnfoldPath(field)
                        hierarchyPath.unfoldHierarchy(receiverWrapper, this)
                    }
                    Stmt.assign(
                        result.toLocalVarUse(), Exp.FieldAccess(receiverViper, field.toViper(), source.asPosition)
                    )
                }
            }
        }
    }

    private fun Sequence<ClassTypeEmbedding>?.unfoldHierarchy(receiverWrapper: ExpEmbedding, ctx: LinearizationContext) {
        this?.forEach { classType ->
            val predAcc = classType.predicateAccess(receiverWrapper, ctx)
            predAcc.let { ctx.addStatement { Stmt.Unfold(it) } }
        }
    }

    private fun ClassTypeEmbedding.predicateAccess(
        receiver: ExpEmbedding,
        ctx: LinearizationContext
    ): Exp.PredicateAccess =
        sharedPredicateAccessInvariant()?.fillHole(receiver)
            ?.pureToViper(toBuiltin = true, ctx.source) as? Exp.PredicateAccess
            ?: error("Attempt to unfold a predicate of ${name.debugMangled}.")


    override fun resolveVariableName(name: SymbolicName): SymbolicName = name
}