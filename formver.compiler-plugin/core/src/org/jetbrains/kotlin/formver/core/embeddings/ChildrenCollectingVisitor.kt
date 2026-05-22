/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings

import org.jetbrains.kotlin.formver.core.embeddings.expression.*

/**
 * Single source of truth for the traversable sub-expressions of each [ExpEmbedding].
 *
 * Every [ExpEmbedding] node's `children()` derives from the corresponding arm here.
 * Adding a new traversable field is a one-place edit: extend the arm for that node.
 */
internal object ChildrenCollectingVisitor : ExpVisitor<List<ExpEmbedding>> {
    override fun visitDefault(e: ExpEmbedding): List<ExpEmbedding> = emptyList()

    override fun visitBlock(e: Block): List<ExpEmbedding> = e.exps
    override fun visitFunctionExp(e: FunctionExp): List<ExpEmbedding> = listOf(e.body)
    override fun visitGotoChainNode(e: GotoChainNode): List<ExpEmbedding> = listOf(e.exp)
    override fun visitIf(e: If): List<ExpEmbedding> = listOf(e.condition, e.thenBranch, e.elseBranch)
    override fun visitElvis(e: Elvis): List<ExpEmbedding> = listOf(e.left, e.right)
    override fun visitReturn(e: Return): List<ExpEmbedding> = listOf(e.returnExp)
    override fun visitMethodCall(e: MethodCall): List<ExpEmbedding> = e.args
    override fun visitFunctionCall(e: FunctionCall): List<ExpEmbedding> = e.args
    override fun visitNonDeterministically(e: NonDeterministically): List<ExpEmbedding> = listOf(e.exp)
    override fun visitWhile(e: While): List<ExpEmbedding> = listOf(e.condition, e.body) + e.invariants
    override fun visitInvokeFunctionObject(e: InvokeFunctionObject): List<ExpEmbedding> =
        listOf(e.receiver) + e.args

    override fun visitFieldAccess(e: FieldAccess): List<ExpEmbedding> = listOf(e.receiver)
    override fun visitPrimitiveFieldAccess(e: PrimitiveFieldAccess): List<ExpEmbedding> = listOf(e.inner)
    override fun visitFieldModification(e: FieldModification): List<ExpEmbedding> = listOf(e.receiver, e.newValue)
    override fun visitFieldAccessPermissions(e: FieldAccessPermissions): List<ExpEmbedding> = listOf(e.inner)
    override fun visitPredicateAccessPermissions(e: PredicateAccessPermissions): List<ExpEmbedding> = e.args
    override fun visitAccEmbedding(e: AccEmbedding): List<ExpEmbedding> = listOf(e.access)

    override fun visitAssign(e: Assign): List<ExpEmbedding> = listOf(e.lhs, e.rhs)
    override fun visitDeclare(e: Declare): List<ExpEmbedding> = listOfNotNull(e.variable, e.initializer)

    override fun visitCast(e: Cast): List<ExpEmbedding> = listOf(e.inner)
    override fun visitIs(e: Is): List<ExpEmbedding> = listOf(e.inner)
    override fun visitSafeCast(e: SafeCast): List<ExpEmbedding> = listOf(e.exp)
    // TODO: include InhaleInvariants.exp once we resolve the purity-checker false
    // positives on inlined function bodies surfaced by validating Asserts nested
    // inside an InhaleInvariants subtree. Including exp here unhides Asserts that
    // the original `children() = emptySequence()` was silently skipping, and the
    // current purity logic flags compiler-introduced Declare(return-slot, null)
    // and InhaleInvariants in the inlined body as impure.

    override fun visitEqCmp(e: EqCmp): List<ExpEmbedding> = listOf(e.left, e.right)
    override fun visitNeCmp(e: NeCmp): List<ExpEmbedding> = listOf(e.left, e.right)
    override fun visitBinaryOperatorExpEmbedding(e: BinaryOperatorExpEmbedding): List<ExpEmbedding> =
        listOf(e.left, e.right)
    override fun visitUnaryOperatorExpEmbedding(e: UnaryOperatorExpEmbedding): List<ExpEmbedding> =
        listOf(e.inner)
    override fun visitSequentialAnd(e: SequentialAnd): List<ExpEmbedding> = listOf(e.left, e.right)
    override fun visitSequentialOr(e: SequentialOr): List<ExpEmbedding> = listOf(e.left, e.right)

    override fun visitWithPosition(e: WithPosition): List<ExpEmbedding> = listOf(e.inner)
    override fun visitSharingContext(e: SharingContext): List<ExpEmbedding> = listOf(e.inner)
    override fun visitShared(e: Shared): List<ExpEmbedding> = listOf(e.inner)

    override fun visitAssert(e: Assert): List<ExpEmbedding> = listOf(e.exp)
    override fun visitInhaleDirect(e: InhaleDirect): List<ExpEmbedding> = listOf(e.exp)
    override fun visitOld(e: Old): List<ExpEmbedding> = listOf(e.inner)
    override fun visitForAllEmbedding(e: ForAllEmbedding): List<ExpEmbedding> = e.conditions
}
