/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.purity

import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.expression.*

/**
 * An [ExpVisitor] that classifies every [ExpEmbedding] node as pure (`true`) or
 * impure (`false`).
 *
 * Purity rules (summary):
 * - Literals, variable reads, and function calls are always pure.
 * - [Declare] is pure only when it includes an initializer; the declared variable is
 *   then tracked in [declaredVariables] so that a subsequent [Assign] to that same
 *   variable is also considered pure (initialisation pattern).
 * - Structural nodes ([Block], [Return], binary/unary operators, [If], …) are pure
 *   iff all their children are pure.
 * - Side-effecting nodes ([MethodCall], field reads/writes, [While], [Goto], …) are
 *   always impure.
 *
 * Use the [isPure] extension rather than instantiating this class directly.
 *
 * @param declaredVariables Mutable set of variables that have been declared-with-initialiser
 *   within the current traversal; pre-populated when re-entering an already-started walk.
 */
internal class ExprPurityVisitor(val declaredVariables: MutableSet<VariableEmbedding> = mutableSetOf()) :
    ExpVisitor<Boolean> {

    /* ————— pure nodes ————— */
    override fun visitUnitLit(e: UnitLit) = true
    override fun visitFunctionCall(e: FunctionCall) = true
    override fun visitDeclare(e: Declare): Boolean {
        val pure = e.initializer != null
        if (pure) declaredVariables.add(e.variable)
        return pure
    }

    override fun visitLiteralEmbedding(e: LiteralEmbedding) = true
    override fun visitExpWrapper(e: ExpWrapper) = true
    override fun visitVariableEmbedding(e: VariableEmbedding) = true
    override fun visitAssign(e: Assign): Boolean =
        e.lhs.ignoringMetaNodes() is VariableEmbedding && declaredVariables.contains(e.lhs.ignoringMetaNodes())

    /* ————— structural nodes without side effects ————— */
    override fun visitReturn(e: Return) = e.allChildrenPure(this)
    override fun visitBlock(e: Block) = e.allChildrenPure(this)
    override fun visitBinaryOperatorExpEmbedding(e: BinaryOperatorExpEmbedding) = e.allChildrenPure(this)
    override fun visitSequentialAnd(e: SequentialAnd) = e.allChildrenPure(this)
    override fun visitSequentialOr(e: SequentialOr) = e.allChildrenPure(this)
    override fun visitEqCmp(e: EqCmp) = e.allChildrenPure(this)
    override fun visitNeCmp(e: NeCmp) = e.allChildrenPure(this)
    override fun visitUnaryOperatorExpEmbedding(e: UnaryOperatorExpEmbedding) = e.allChildrenPure(this)
    override fun visitWithPosition(e: WithPosition) = e.allChildrenPure(this)
    override fun visitInjectionBasedExpEmbedding(e: InjectionBasedExpEmbedding) = e.allChildrenPure(this)
    override fun visitSharingContext(e: SharingContext) = e.allChildrenPure(this)
    override fun visitIf(e: If) = e.allChildrenPure(this)

    override fun visitElvis(e: Elvis) = false
    override fun visitSafeCast(e: SafeCast) = false
    override fun visitCast(e: Cast) = false
    override fun visitIs(e: Is) = false
    override fun visitOld(e: Old) = false
    override fun visitForAllEmbedding(e: ForAllEmbedding) = false


    /* ————— impure nodes ————— */
    override fun visitMethodCall(e: MethodCall) = false// TODO: Whitelist for annotated methods?
    override fun visitFunctionExp(e: FunctionExp) = false
    override fun visitLambdaExp(e: LambdaExp) = false
    override fun visitInvokeFunctionObject(e: InvokeFunctionObject) = false
    override fun visitShared(e: Shared) = false
    override fun visitInhaleDirect(e: InhaleDirect): Boolean = false
    override fun visitErrorExp(e: ErrorExp) = false

    override fun visitAssert(e: Assert): Boolean = false
    override fun visitFieldModification(e: FieldModification): Boolean = false
    override fun visitFieldAccess(e: FieldAccess): Boolean = false // TODO
    override fun visitPrimitiveFieldAccess(e: PrimitiveFieldAccess): Boolean = false // TODO
    override fun visitGoto(e: Goto): Boolean = false
    override fun visitGotoChainNode(e: GotoChainNode): Boolean = false
    override fun visitWhile(e: While): Boolean = false
    override fun visitNonDeterministically(e: NonDeterministically): Boolean = false
    override fun visitInhaleInvariants(e: InhaleInvariants): Boolean = false
    override fun visitFieldAccessPermissions(e: FieldAccessPermissions): Boolean = false
    override fun visitPredicateAccessPermissions(e: PredicateAccessPermissions): Boolean = false
    override fun visitLabelExp(e: LabelExp): Boolean = false

    override fun visitAccEmbedding(e: AccEmbedding): Boolean = false

    override fun visitDefault(e: ExpEmbedding): Boolean = false
}

private fun ExpEmbedding.allChildrenPure(v: ExprPurityVisitor): Boolean =
    children().all { it.accept(v) }