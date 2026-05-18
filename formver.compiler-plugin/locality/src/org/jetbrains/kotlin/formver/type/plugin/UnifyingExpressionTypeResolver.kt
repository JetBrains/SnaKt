/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.lastExpression
import org.jetbrains.kotlin.utils.yieldIfNotNull

/**
 * Strips [FirExpression] from cast operations.
 */
fun FirExpression.removeCast(): FirExpression =
    when (this) {
        is FirTypeOperatorCall -> when (operation) {
            FirOperation.AS, FirOperation.SAFE_AS ->
                argument.unwrapExpression().removeCast()
            else -> this
        }
        else -> this
    }

/**
 * Collects all the tails of a conditional expression. Note that these tails do not include the expression itself. If
 * the expression is not conditional it returns an empty sequence.
 */
fun FirExpression.collectTails(): Sequence<FirExpression> =
    when (val expression = unwrapExpression()) {
        is FirWhenExpression ->
            sequence {
                yieldAll(expression.branches.map(FirWhenBranch::result))
            }
        is FirTryExpression ->
            sequence {
                yield(expression.tryBlock)
                yieldAll(expression.catches.map(FirCatch::block))
            }
        is FirBlock ->
            sequence {
                yieldIfNotNull(expression.lastExpression)
            }
        else ->
            emptySequence()
    }

/**
 * Resolves the type of an expression by unifying the types of its tail subexpressions.
 *
 * @param Type the type class of the expression.
 * @param cachesFactory the factory for creating the cache for storing the subexpression results.
 * @param typeUnifier the type unifier to use for unifying the types of the expression tails.
 * @param terminalTypeResolver the resolver for resolving the type of the expression if it has no tail subexpressions.
 */
class UnifyingExpressionTypeResolver<Type>(
    cachesFactory: FirCachesFactory,
    private val typeUnifier: TypeUnifier<Type>,
    private val terminalTypeResolver: ExpressionTypeResolver<Type>
) : ExpressionTypeResolver<Type> {
    private val cache = cachesFactory.createCache { expression: FirExpression, context: CheckerContext ->
        with(context) {
            extractTypeOf(expression)
        }
    }

    context(context: CheckerContext)
    override fun resolveTypeOf(expression: FirExpression): Type =
        cache.getValue(expression, context)

    context(context: CheckerContext)
    private fun extractTypeOf(expression: FirExpression): Type {
        val expression = expression.unwrapExpression().removeCast()
        val tails = expression.collectTails()

        if (!tails.any()) {
            return terminalTypeResolver.resolveTypeOf(expression)
        } else {
            return tails.map { resolveTypeOf(it) }.reduce(typeUnifier::join)
        }
    }
}
