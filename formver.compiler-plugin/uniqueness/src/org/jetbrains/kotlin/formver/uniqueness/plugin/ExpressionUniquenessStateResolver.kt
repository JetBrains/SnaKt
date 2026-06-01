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
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.type.plugin.ExpressionTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.UnifyingExpressionTypeResolver

private val FirQualifiedAccessExpression.pathReceiver: FirExpression?
    get() = explicitReceiver ?: extensionReceiver ?: dispatchReceiver

private fun AccessState.asReceiverState(): AccessState =
    copy(data = false)

object TerminalAccessResolver : ExpressionTypeResolver<AccessState> {
    context(context: CheckerContext)
    override fun resolveTypeOf(expression: FirExpression): AccessState =
        when (expression) {
            is FirQualifiedAccessExpression -> {
                when (val symbol = expression.calleeReference.symbol) {
                    is FirVariableSymbol<*> -> {
                        val receiverState = expression.pathReceiver
                            ?.resolveAccessState()
                            ?.asReceiverState()
                            ?: AccessState(false)

                        receiverState.append(symbol, AccessState(true))
                    }

                    else -> EmptyAccessState
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
        TerminalAccessResolver
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

/**
 * Resolves the uniqueness environment (input [UniquenessState]) at a given expression by looking it
 * up in the mapping produced by [GraphUniquenessStateMappingResolver].
 *
 * This component walks the [CheckerContext.containingDeclarations] to find the nearest enclosing
 * function's CFG, builds the mapping once (cached), and then looks up the given expression.
 */
class ExpressionUniquenessEnvironmentResolver(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory =
            Factory { session -> ExpressionUniquenessEnvironmentResolver(session) }

        context(context: CheckerContext)
        fun resolveInputEnvironmentOf(
            expression: FirExpression,
            function: FirFunctionSymbol<*>? = null
        ): UniquenessState =
            context.session.expressionUniquenessEnvironmentResolver.resolveInputEnvironmentOf(
                expression.unwrapExpression(),
                function
            )

        context(context: CheckerContext)
        fun resolveOutputEnvironmentOf(expression: FirExpression): UniquenessState =
            context.session.expressionUniquenessEnvironmentResolver.resolveOutputEnvironmentOf(expression.unwrapExpression())
    }

    context(context: CheckerContext)
    fun resolveInputEnvironmentOf(expression: FirExpression, function: FirFunctionSymbol<*>? = null): UniquenessState {
        val symbol = function ?: context.containingDeclarations.asReversed()
            .firstOrNull { it is FirFunctionSymbol<*> } as? FirFunctionSymbol<*> ?: return EmptyUniquenessState
        val graph = symbol.resolvedControlFlowGraphReference?.controlFlowGraph ?: return EmptyUniquenessState
        val mapping = session.graphUniquenessStateMappingResolver.resolveMappingOf(graph)
        val pair = mapping[expression]
        if (pair != null) return pair.input

        return EmptyUniquenessState
    }

    context(context: CheckerContext)
    fun resolveOutputEnvironmentOf(expression: FirExpression): UniquenessState {
        for (symbol in context.containingDeclarations.asReversed()) {
            if (symbol !is FirFunctionSymbol<*>) continue
            val graph = symbol.resolvedControlFlowGraphReference?.controlFlowGraph ?: continue
            val mapping = session.graphUniquenessStateMappingResolver.resolveMappingOf(graph)
            val pair = mapping[expression]
            if (pair != null) return pair.output
        }
        return EmptyUniquenessState
    }
}

private val FirSession.expressionUniquenessEnvironmentResolver: ExpressionUniquenessEnvironmentResolver
        by FirSession.sessionComponentAccessor()

private val FirSession.graphUniquenessStateMappingResolver: GraphUniquenessStateMappingResolver
        by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun FirExpression.resolveInputUniquenessEnvironment(function: FirFunctionSymbol<*>? = null): UniquenessState =
    ExpressionUniquenessEnvironmentResolver.resolveInputEnvironmentOf(this, function)

context(context: CheckerContext)
fun FirExpression.resolveOutputUniquenessEnvironment(): UniquenessState =
    ExpressionUniquenessEnvironmentResolver.resolveInputEnvironmentOf(this)
