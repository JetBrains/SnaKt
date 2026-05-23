/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.formver.type.plugin.ExpressionTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.ReturnResultTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.ThrowExceptionTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.collectTails
import org.jetbrains.kotlin.formver.type.plugin.removeCast
import org.jetbrains.kotlin.types.ConstantValueKind

/**
 * Terminal resolver that computes the uniqueness of an expression that has no conditional tails.
 *
 * It considers:
 * - The access trie filtered against the current uniqueness environment.
 * - `null` literals are always unique.
 * - Constructor calls are always unique.
 * - Otherwise falls back to [Uniqueness.Shared].
 */
private object TerminalUniquenessResolver : ExpressionTypeResolver<Uniqueness> {
    context(context: CheckerContext)
    override fun resolveTypeOf(expression: FirExpression): Uniqueness {
        val environment = expression.resolveUniquenessEnvironment()

        return when (expression) {
            is FirLiteralExpression ->
                if (expression.kind == ConstantValueKind.Null) Uniqueness.Unique
                else Uniqueness.Shared

            is FirFunctionCall ->
                if (expression.calleeReference.symbol is FirConstructorSymbol) Uniqueness.Unique
                else Uniqueness.Shared

            is FirPropertyAccessExpression -> {
                expression.resolveAccessState().mask(environment)
                    .asUniqueness()
            }

            else -> Uniqueness.Unique
        }
    }
}

/**
 * Resolves the uniqueness of an expression using the flow-sensitive uniqueness environment.
 *
 * Because the uniqueness result depends on a flow-sensitive environment that varies per program
 * point, this resolver does **not** cache results at the session level. Instead, it performs a
 * fresh walk through tails on each invocation, delegating terminal resolution to
 * [TerminalUniquenessResolver].
 */
object ExpressionUniquenessResolver : ExpressionTypeResolver<Uniqueness> {
    context(context: CheckerContext)
    private fun extractTypeOf(expression: FirExpression): Uniqueness {
        val unwrapped = expression.unwrapExpression().removeCast()
        val tails = unwrapped.collectTails()

        return if (!tails.any()) {
            TerminalUniquenessResolver.resolveTypeOf(unwrapped)
        } else {
            tails.map { extractTypeOf(it) }.reduce(UniquenessUnifier::join)
        }
    }

    context(context: CheckerContext)
    override fun resolveTypeOf(expression: FirExpression): Uniqueness =
        extractTypeOf(expression)
}

object ExpressionDefaultUniquenessResolver : ExpressionTypeResolver<Uniqueness> {
    context(context: CheckerContext)
    private fun extractTypeOf(expression: FirExpression): Uniqueness {
        return expression.resolveAccessState().resolveUniqueness()
    }

    context(context: CheckerContext)
    override fun resolveTypeOf(expression: FirExpression): Uniqueness =
        extractTypeOf(expression)
}

object ReturnResultUniquenessResolver : ReturnResultTypeResolver<Uniqueness> {
    context(context: CheckerContext)
    override fun resolveResultTypeOf(expression: FirReturnExpression): Uniqueness = Uniqueness.Shared
}

object ThrowExceptionUniquenessResolver : ThrowExceptionTypeResolver<Uniqueness> {
    context(context: CheckerContext)
    override fun resolveExceptionTypeOf(expression: FirThrowExpression): Uniqueness = Uniqueness.Shared
}
