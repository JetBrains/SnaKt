/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.formver.type.plugin.ExpressionTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.collectTails
import org.jetbrains.kotlin.formver.type.plugin.removeCast
import org.jetbrains.kotlin.types.ConstantValueKind

class TerminalUniquenessResolver(
    private val uniquenessState: UniquenessState,
) : ExpressionTypeResolver<Uniqueness> {
    context(context: CheckerContext)
    override fun resolveTypeOf(expression: FirExpression): Uniqueness {
        return when (expression) {
            is FirLiteralExpression ->
                if (expression.kind == ConstantValueKind.Null) Uniqueness.Unique
                else Uniqueness.Shared

            is FirFunctionCall ->
                if (expression.calleeReference.symbol is FirConstructorSymbol) Uniqueness.Unique
                else Uniqueness.Shared

            is FirPropertyAccessExpression -> {
                val accessState = expression.resolveAccessState()
                accessState
                    .filter(uniquenessState)
                    .joinChildren(UniquenessUnifier)
            }

            else -> Uniqueness.Shared
        }
    }
}

class ExpressionUniquenessResolver(session: FirSession) :
    FirExtensionSessionComponent(session) {
    private val cache = session.firCachesFactory.createCache { input: Pair<UniquenessState, FirExpression>, context: CheckerContext ->
        with(context) {
            extractTypeOf(input.second, input.first)
        }
    }

    companion object {
        fun getFactory(): Factory {
            return Factory { session -> ExpressionUniquenessResolver(session) }
        }
    }

    context(context: CheckerContext)
    fun resolveTypeOf(expression: FirExpression, state: UniquenessState): Uniqueness =
        cache.getValue(state to expression, context)

    context(context: CheckerContext)
    private fun extractTypeOf(expression: FirExpression, state: UniquenessState): Uniqueness {
        val unwrappedExpression = expression.unwrapExpression().removeCast()
        val tails = unwrappedExpression.collectTails()

        return if (!tails.any()) {
            TerminalUniquenessResolver(state).resolveTypeOf(unwrappedExpression)
        } else {
            tails.map { resolveTypeOf(it, state) }.reduce(UniquenessUnifier::join)
        }
    }
}

private val FirSession.expressionUniquenessResolver: ExpressionUniquenessResolver
    by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun FirExpression.resolveUniqueness(state: UniquenessState): Uniqueness =
    context.session.expressionUniquenessResolver.resolveTypeOf(this, state)
