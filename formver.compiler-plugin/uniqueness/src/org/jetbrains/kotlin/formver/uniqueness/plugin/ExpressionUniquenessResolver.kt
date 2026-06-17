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
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNothingOrNullableNothing
import org.jetbrains.kotlin.fir.types.isPrimitive
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.formver.type.plugin.ExpressionTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.ReturnResultTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.ThrowExceptionTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.UnifyingExpressionTypeResolver

context(context: CheckerContext)
fun FirExpression.resolveAccessUniqueness(): Uniqueness {
    val accessState = resolveAccessState()

    if (accessState == EmptyAccessState) return Uniqueness.Shared

    val uniquenessState = resolveInputUniquenessState() ?: EmptyUniquenessState

    return accessState.joinUniquenessOverTerminals(uniquenessState)
}

private object TerminalUniquenessResolver : ExpressionTypeResolver<Uniqueness> {
    context(context: CheckerContext)
    override fun resolveTypeOf(expression: FirExpression): Uniqueness {
        return when (expression) {
            is FirFunctionCall ->
                if (expression.calleeReference.symbol is FirConstructorSymbol) Uniqueness.Unique
                else expression.resolvedType.defaultUniqueness

            is FirThisReceiverExpression -> {
                expression.resolveAccessUniqueness()
            }

            is FirPropertyAccessExpression -> {
                val receiverUniqueness = expression.pathReceiver?.resolveUniqueness() ?: Uniqueness.Unique

                receiverUniqueness.join(expression.resolveAccessUniqueness())
            }

            is FirSafeCallExpression -> {
                val receiver = expression.receiver
                val receiverUniqueness = receiver.resolveUniqueness()

                receiverUniqueness.join(expression.resolveAccessUniqueness())
            }

            else -> {
                val resolvedType = expression.resolvedType

                if (resolvedType.isNothingOrNullableNothing || resolvedType.isPrimitive) {
                    Uniqueness.Unique
                } else {
                    Uniqueness.Shared
                }
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
 * Extracts the actual uniqueness from this access state.
 */
context(context: CheckerContext)
fun FirExpression.resolveUniqueness(): Uniqueness =
    context.session.expressionUniquenessResolver.resolveTypeOf(this)

/**
 * Extracts the default uniqueness from this access state.
 */
context(context: CheckerContext)
fun FirExpression.resolveDefaultUniqueness(): Uniqueness {
    val accessState = resolveAccessState()

    return if (accessState == EmptyAccessState) {
        Uniqueness.Shared
    } else {
        accessState.symbols.fold(Uniqueness.Unique) { result, symbol ->
            result.join(symbol.resolveDeclaredUniqueness())
        }
    }
}

object ExpressionDefaultUniquenessResolver : ExpressionTypeResolver<Uniqueness> {
    context(context: CheckerContext)
    override fun resolveTypeOf(expression: FirExpression): Uniqueness =
        expression.resolveDefaultUniqueness()
}

object ReturnResultUniquenessResolver : ReturnResultTypeResolver<Uniqueness> {
    context(context: CheckerContext)
    override fun resolveResultTypeOf(expression: FirReturnExpression): Uniqueness =
        expression.target.labeledElement.returnTypeRef.coneType.defaultUniqueness
}

object ThrowExceptionUniquenessResolver : ThrowExceptionTypeResolver<Uniqueness> {
    context(context: CheckerContext)
    override fun resolveExceptionTypeOf(expression: FirThrowExpression): Uniqueness = Uniqueness.Shared
}
