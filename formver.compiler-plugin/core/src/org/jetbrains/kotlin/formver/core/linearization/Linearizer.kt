/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.core.conversion.ReturnTarget
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.expression.AnonymousVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpWrapper
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.toLink
import org.jetbrains.kotlin.formver.core.embeddings.toViperGoto
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.predicateAccess
import org.jetbrains.kotlin.formver.core.embeddings.types.uniquePredicateAccessOrNull
import org.jetbrains.kotlin.formver.names.debugMangled
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Position
import org.jetbrains.kotlin.formver.viper.ast.Stmt

/**
 * Standard context for linearization.
 */
data class Linearizer(
    val state: SharedLinearizationState,
    val seqnBuilder: SeqnBuilder,
    override val source: KtSourceElement?,
    override val typeResolver: TypeResolver,
    val stmtModifierTracker: StmtModifierTracker? = null
) : LinearizationContext {
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

    override fun store(lhs: VariableEmbedding, rhs: Linearizable) {
        val lhsViper = lhs.toViperExp(this)
        val rhsViper = rhs.toViper(this)
        addStatement { Stmt.assign(lhsViper, rhsViper, source.asPosition) }
    }

    override fun addReturn(returnExp: Linearizable, target: ReturnTarget) {
        returnExp.toViperStoringIn(target.variable, this)
        // Fold any function-scope unfolded predicates before the goto so the predicate
        // postcondition holds at the return target. The fold itself doesn't disturb the
        // returnExp evaluation — it ran above and stored its result already.
        foldUsedFunctionScopedReceivers()
        addStatement { target.label.toLink().toViperGoto(this) }
    }

    override fun registerEligibleReceiver(receiverName: SymbolicName, predAcc: Exp.PredicateAccess) {
        state.preUnfoldedReceivers.add(PreUnfoldedReceiver(receiverName, predAcc))
    }

    override fun tryUseFunctionScopedReceiver(receiverName: SymbolicName): Boolean {
        val entry = state.preUnfoldedReceivers.firstOrNull { it.receiverName == receiverName } ?: return false
        if (!entry.unfolded) {
            addStatement { Stmt.Unfold(entry.predicateAccess, source.asPosition) }
            entry.unfolded = true
        }
        return true
    }

    override fun foldUsedFunctionScopedReceivers() {
        for (entry in state.preUnfoldedReceivers) {
            if (!entry.unfolded) continue
            addStatement { Stmt.Fold(entry.predicateAccess, source.asPosition) }
        }
    }

    override fun addBranch(
        condition: Linearizable,
        thenBranch: Linearizable,
        elseBranch: Linearizable,
        result: VariableEmbedding?
    ) =
        addStatement {
            val condViper = condition.toViperBuiltinType(this)
            val thenViper = asBlock { thenBranch.toViperMaybeStoringIn(result, this) }
            val elseViper = asBlock { elseBranch.toViperMaybeStoringIn(result, this) }
            Stmt.If(condViper, thenViper, elseViper, source.asPosition)
        }

    override fun addFieldAccess(
        receiver: Linearizable,
        receiverType: TypeEmbedding,
        field: FieldEmbedding,
        receiverIsUnique: Boolean,
    ): Exp {
        val result = freshAnonVar(field.type)
        addFieldAccessStoringIn(receiver, receiverType, field, result, receiverIsUnique)
        return result.toViperExp(this)
    }

    override fun addModifier(mod: StmtModifier) {
        stmtModifierTracker?.add(mod) ?: error("Not in a statement")
    }

    override fun addFieldAccessStoringIn(
        receiver: Linearizable,
        receiverType: TypeEmbedding,
        field: FieldEmbedding,
        result: VariableEmbedding,
        receiverIsUnique: Boolean,
    ) {
        if (field.accessPolicy == AccessPolicy.BY_RECEIVER_UNIQUENESS && receiverIsUnique) {
            if (emitUniqueReceiverFieldRead(receiver, receiverType, field, result)) return
            // fall through to the conservative havoc on the rare cases where we don't have a
            // unique-predicate handle (e.g. multi-step hierarchy not yet plumbed).
        }
        addStatement {
            when (field.accessPolicy) {
                AccessPolicy.BY_RECEIVER_UNIQUENESS -> {
                    // TODO: Handling a unique field on a shared receiver must be added here.
                    receiver.toViperUnusedResult(this)
                    field.type.havocMethod(typeResolver).toMethodCall(emptyList(), listOf(result.toLocalVarUse()))
                }

                else -> {
                    val receiverViper = receiver.toViper(this)
                    // If the field access is not replaced with havoc,
                    // we might need to unfold some predicate to access it.
                    if (field.unfoldToAccess) {
                        val receiverWrapper = ExpWrapper(receiverViper, receiverType)
                        val hierarchyPath = typeResolver.hierarchyPathTo(receiverType.pretype, field)
                        hierarchyPath.unfoldHierarchyPath(receiverWrapper, this)
                    }
                    Stmt.assign(
                        result.toLocalVarUse(), Exp.FieldAccess(receiverViper, field.toViper(), source.asPosition)
                    )
                }
            }
        }
    }

    /**
     * Read a `var` field through a unique receiver without dropping the value to havoc.
     * The unique predicate carries `acc(receiver.field, write)`, so we unfold it, perform a
     * direct field-read, and fold it back so subsequent code (and other unique reads) still
     * holds the predicate. Returns false if we can't resolve a unique-predicate handle for
     * this field's containing class — in that case the caller falls back to havoc.
     *
     * Limited to single-step hierarchy: when the field lives in a strict supertype, we punt
     * to the conservative path. Deeper hierarchies want the same fold/unfold chain that the
     * shared-predicate path already does, just with full perm and an inverse-order refold.
     */
    private fun emitUniqueReceiverFieldRead(
        receiver: Linearizable,
        receiverType: TypeEmbedding,
        field: FieldEmbedding,
        result: VariableEmbedding,
    ): Boolean {
        val classType = receiverType.pretype as? ClassTypeEmbedding ?: return false
        val hierarchyPath = typeResolver.hierarchyPathTo(receiverType.pretype, field).toList()
        if (hierarchyPath.size != 1 || hierarchyPath.single() !== classType) return false

        addStatement {
            val receiverViper = receiver.toViper(this)
            val functionScoped = receiverViper is Exp.LocalVar &&
                    tryUseFunctionScopedReceiver(receiverViper.name)
            if (functionScoped) {
                // Function-scope unfold is in place (or was just emitted by the lazy hook
                // on this very access); just emit the read.
                Stmt.assign(
                    result.toLocalVarUse(),
                    Exp.FieldAccess(receiverViper, field.toViper(), source.asPosition)
                )
            } else {
                val receiverWrapper = ExpWrapper(receiverViper, receiverType)
                val uniquePredAcc = classType.uniquePredicateAccessOrNull(receiverWrapper, typeResolver, source)
                    ?: error("ClassTypeEmbedding ${classType.name.debugMangled} has no unique-predicate access")
                addStatement { Stmt.Unfold(uniquePredAcc, source.asPosition) }
                addStatement {
                    Stmt.assign(
                        result.toLocalVarUse(),
                        Exp.FieldAccess(receiverViper, field.toViper(), source.asPosition)
                    )
                }
                Stmt.Fold(uniquePredAcc, source.asPosition)
            }
        }
        return true
    }

    private fun Sequence<ClassTypeEmbedding>?.unfoldHierarchyPath(
        receiverWrapper: ExpEmbedding,
        ctx: LinearizationContext
    ) {
        this?.forEach { classType ->
            val predAcc = classType.predicateAccess(receiverWrapper, typeResolver, source)
            ctx.addStatement { Stmt.Unfold(predAcc, source.asPosition) }
        }
    }

    override fun resolveVariableName(name: SymbolicName): SymbolicName = name
}
