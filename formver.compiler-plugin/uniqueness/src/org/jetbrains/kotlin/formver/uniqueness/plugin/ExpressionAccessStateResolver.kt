/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.type.plugin.ExpressionTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.UnifyingExpressionTypeResolver

/**
 * Resolves the receiver of [this] qualified access expression targeting a property.
 *
 * NOTE: this property shouldn't be used to find the receiver of a qualified access expression targeting anything other
 * than a plain property or local variable.
 */
val FirQualifiedAccessExpression.pathReceiver: FirExpression?
    get() = explicitReceiver ?: extensionReceiver ?: dispatchReceiver

object TerminalAccessStateResolver : ExpressionTypeResolver<AccessState> {
    context(context: CheckerContext)
    override fun resolveTypeOf(expression: FirExpression): AccessState =
        when (expression) {
            is FirQualifiedAccessExpression -> {
                when (val symbol = expression.calleeReference.symbol) {
                    is FirVariableSymbol<*> -> {
                        val receiverState = expression.pathReceiver
                            ?.resolveAccessState()
                            ?: EmptyAccessState

                        receiverState.append(EmptyAccessState.associate(symbol, AccessState(Access.Terminal)))
                    }
                    else -> EmptyAccessState
                }
            }
            is FirSafeCallExpression -> {
                val selector = expression.selector

                return if (selector is FirExpression) {
                    selector.resolveAccessState()
                } else {
                    EmptyAccessState
                }
            }
            else -> EmptyAccessState
        }
}

class ExpressionAccessStateResolver(session: FirSession) :
    FirExtensionSessionComponent(session),
    ExpressionTypeResolver<AccessState> by UnifyingExpressionTypeResolver(
        session.firCachesFactory,
        AccessStateUnifier,
        TerminalAccessStateResolver
    ) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> ExpressionAccessStateResolver(session) }
        }
    }
}

private val FirSession.expressionAccessStateResolver: ExpressionAccessStateResolver
        by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun FirExpression.resolveAccessState(): AccessState {
    return context.session.expressionAccessStateResolver.resolveTypeOf(this)
}
