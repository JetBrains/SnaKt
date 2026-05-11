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
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.resolvedType

class ExpressionLocalityResolver(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> ExpressionLocalityResolver(session) }
        }
    }

    private val cacheFactory = session.firCachesFactory

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext)
    private fun extractLocalityOf(expression: FirExpression): LocalityAttribute? {
        val expression = expression.unwrapCast()

        return when (expression) {
            is FirPropertyAccessExpression ->
                (expression.calleeReference.symbol as? FirVariableSymbol<*>)?.fir?.resolveLocality()
            else -> {
                expression.resolvedType.locality ?:
                expression.collectTails()
                    .map { tail -> resolveLocalityOf(tail) }
                    .reduceOrNull { result, locality -> result?.union(locality) }
            }
        }
    }

    private val cache = cacheFactory.createCache { expression: FirExpression, context: CheckerContext ->
        with(context) {
            extractLocalityOf(expression)
        }
    }

    context(context: CheckerContext)
    fun resolveLocalityOf(expression: FirExpression): LocalityAttribute? =
        cache.getValue(expression, context)
}

private val FirSession.expressionLocalityResolver: ExpressionLocalityResolver by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun FirExpression.resolveLocality(): LocalityAttribute? =
    context.session.expressionLocalityResolver.resolveLocalityOf(this)
