/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.isNothing
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.formver.type.plugin.ExpressionTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.ReturnResultTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.ThrowExceptionTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.UnifyingExpressionTypeResolver
import org.jetbrains.kotlin.types.ConstantValueKind

private object TerminalUniquenessResolver : ExpressionTypeResolver<Uniqueness> {
    context(context: CheckerContext)
    override fun resolveTypeOf(expression: FirExpression): Uniqueness {
        val environment = expression.resolveInputUniquenessEnvironment()

        return when (expression) {
            is FirLiteralExpression ->
                if (expression.kind == ConstantValueKind.Null) Uniqueness.Unique
                else Uniqueness.Shared

            is FirFunctionCall ->
                if (expression.calleeReference.symbol is FirConstructorSymbol) Uniqueness.Unique
                else Uniqueness.Shared

            is FirThisReceiverExpression -> {
                val symbol = expression.calleeReference.symbol ?: return Uniqueness.Shared
                val accessState = AccessState(false, persistentMapOf(symbol to AccessState(true)))

                accessState.project(environment).asUniqueness()
            }

            is FirPropertyAccessExpression -> {
                expression.resolveAccessState().project(environment).asUniqueness()
            }

            else ->
                if (expression.resolvedType.isNothing) {
                    Uniqueness.Unique
                } else {
                    Uniqueness.Shared
                }
        }
    }
}

class ExpressionUniquenessResolver(session: FirSession) :
    ExpressionTypeResolver<Uniqueness> by UnifyingExpressionTypeResolver(
        session.firCachesFactory,
        UniquenessUnifier,
        TerminalUniquenessResolver
    ), FirExtensionSessionComponent(session) {
    companion object : ExpressionTypeResolver<Uniqueness> {
        fun getFactory(): Factory {
            return Factory { session -> ExpressionUniquenessResolver(session) }
        }

        context(context: CheckerContext)
        override fun resolveTypeOf(expression: FirExpression): Uniqueness =
            context.session.expressionUniquenessResolver.resolveTypeOf(expression)
    }
}

private val FirSession.expressionUniquenessResolver: ExpressionUniquenessResolver
        by FirSession.sessionComponentAccessor()

/**
 * Extracts the default uniqueness from this access state.
 */
context(context: CheckerContext)
fun AccessState.extractDefaultUniqueness(): Uniqueness =
    if (this == EmptyAccessState) {
        Uniqueness.Shared
    } else {
        symbols.fold(Uniqueness.Unique) { result, symbol ->
            result.join(symbol.resolveComponentUniqueness())
        }
    }

object ExpressionDefaultUniquenessResolver : ExpressionTypeResolver<Uniqueness> {
    context(context: CheckerContext)
    private fun extractTypeOf(expression: FirExpression): Uniqueness {
        return expression.resolveAccessState().extractDefaultUniqueness()
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
