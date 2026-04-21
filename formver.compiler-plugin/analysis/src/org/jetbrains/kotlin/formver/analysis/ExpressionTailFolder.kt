/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.analysis

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedExpression
import org.jetbrains.kotlin.fir.lastExpression
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * Folds a value over each tail of the expression.
 *
 * Expressions that do not produce a usable tail value for the analysis return [empty].
 * By default these include arbitrary FIR elements.
 *
 * This folder can be used to resolve a value based on the branch structure of the expression. Example of these include:
 * if, elvis, and when expressions. For example, in the right-hand side of the following assignment:
 * ```kotlin
 * val v = if (*) {
 *     a
 * } else {
 *     b
 * }
 * ```
 * the result of this visitor will be `a.accept(this, data).join(b.accept(this, data))`.
 *
 * Implementors define the abstract domain [T] by providing:
 * - [empty], the identity / "no tail value" result
 * - [join], the way alternative tail paths are merged
 *
 * In addition to this, they can also override specific visitor methods when a particular expression kind should
 * contribute a non-empty result, as [org.jetbrains.kotlin.formver.locality.ExpressionLocalityExtractor]
 * does for variable, property, and receiver accesses.
 *
 * [D] is the type of the extra data threaded through the traversal.
 */
abstract class ExpressionTailFolder<T, D> : FirVisitor<T, D>() {
    /**
     * Result used when an expression does not produce any relevant value for the tail of the expression.
     *
     * This should act as the identity element for [join].
     */
    abstract val empty: T

    /**
     * Merges results from alternative value-producing tails.
     *
     * Typical examples are the branches of a `when`, the two sides of an Elvis expression, or the `try` block together
     * with all `catch` blocks.
     */
    abstract fun T.join(other: T): T

    protected fun FirExpression?.visit(data: D): T =
        this?.accept(this@ExpressionTailFolder, data) ?: empty

    override fun visitElement(
        element: FirElement,
        data: D
    ): T {
        return empty
    }

    override fun visitBlock(
        block: FirBlock,
        data: D
    ): T {
        return block.lastExpression?.visit(data) ?: empty
    }

    override fun visitWhenExpression(
        whenExpression: FirWhenExpression,
        data: D
    ): T {
        return whenExpression.branches.fold(empty) { result, branch ->
            result.join(branch.result.visit(data))
        }
    }

    override fun visitElvisExpression(
        elvisExpression: FirElvisExpression,
        data: D
    ): T {
        return elvisExpression.lhs.visit(data).join(elvisExpression.rhs.visit(data))
    }

    override fun visitTryExpression(
        tryExpression: FirTryExpression,
        data: D
    ): T {
        val tryValue = tryExpression.tryBlock.visit(data)
        val catchValues = tryExpression.catches.fold(empty) { result, catch ->
            result.join(catch.block.visit(data))
        }

        return tryValue.join(catchValues)
    }
}
