/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

class ExpressionLocalityResolver(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> ExpressionLocalityResolver(session) }
        }
    }

    private val cacheFactory = session.firCachesFactory

    private val cache = cacheFactory.createCache { expression: FirExpression, context: CheckerContext ->
        with(context) {
            extractLocalityOf(expression)
        }
    }

    context(context: CheckerContext)
    fun resolveLocalityOf(expression: FirExpression): Locality =
        cache.getValue(expression, context)

    context(context: CheckerContext)
    fun extractLocalityOf(expression: FirExpression): Locality =
        when (val expression = expression.unwrapExpression().removeCast()) {
            is FirThisReceiverExpression ->
                (expression.calleeReference.symbol as? FirReceiverParameterSymbol)
                    ?.resolveLocality()
            is FirPropertyAccessExpression ->
                (expression.calleeReference.symbol as? FirVariableSymbol)
                    ?.resolveLocality()
            else -> {
                expression.collectTails()
                    .map { tail -> resolveLocalityOf(tail) }
                    .reduceOrNull(Locality::join)
            }
        }
}

private val FirSession.expressionLocalityResolver: ExpressionLocalityResolver by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun FirExpression.resolveLocality(): Locality =
    context.session.expressionLocalityResolver.resolveLocalityOf(this)
