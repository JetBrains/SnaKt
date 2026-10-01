/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.embeddings.ExpVisitor
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.core.linearization.LogicOperatorPolicy

/**
 * In pure contexts these operators can be written as simple binary operators.
 * However, regularly their semantics is different: evaluate the first argument and then maybe the second one (not necessarily)
 */
sealed class SequentialLogicOperatorEmbedding : ExpEmbedding {
    abstract val left: ExpEmbedding
    abstract val right: ExpEmbedding

    override val type
        get() = buildType { boolean() }

    protected abstract val ifReplacement: ExpEmbedding
    protected abstract val expressionReplacement: ExpEmbedding

    fun operatorReplacement(ctx: LinearizationContext) = when (ctx.logicOperatorPolicy) {
        LogicOperatorPolicy.CONVERT_TO_IF -> ifReplacement
        LogicOperatorPolicy.CONVERT_TO_EXPRESSION -> expressionReplacement
    }

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(left, right)
}

data class SequentialAnd(override val left: ExpEmbedding, override val right: ExpEmbedding) :
    SequentialLogicOperatorEmbedding() {
    override val ifReplacement
        get() = If(left, right.withType(type), BooleanLit(false).withType(type), type)
    override val expressionReplacement
        get() = OperatorExpEmbeddings.And(left, right)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitSequentialAnd(this)
}

data class SequentialOr(override val left: ExpEmbedding, override val right: ExpEmbedding) :
    SequentialLogicOperatorEmbedding() {
    override val ifReplacement
        get() = If(left, BooleanLit(true).withType(type), right.withType(type), type)
    override val expressionReplacement
        get() = OperatorExpEmbeddings.Or(left, right)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitSequentialOr(this)
}

/**
 * A call to [org.jetbrains.kotlin.formver.plugin.implies]. Under [LogicOperatorPolicy.CONVERT_TO_IF] it is an
 * ordinary call, so both operands are evaluated left to right before the implication is computed. Under
 * [LogicOperatorPolicy.CONVERT_TO_EXPRESSION] it becomes a Viper implication, which does not evaluate the right
 * operand when the left one is false.
 */
data class ContextualImplies(val left: ExpEmbedding, val right: ExpEmbedding) : ExpEmbedding {
    override val type
        get() = buildType { boolean() }

    override fun children(): Sequence<ExpEmbedding> = sequenceOf(left, right)

    override fun <R> accept(v: ExpVisitor<R>): R = v.visitContextualImplies(this)
}
