/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.resolvedType

class ExpressionLocalityContractResolver(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> ExpressionLocalityContractResolver(session) }
        }
    }

    private val cacheFactory = session.firCachesFactory

    private val cache =
        cacheFactory.createCache { expression: FirExpression, context: CheckerContext ->
            with(context) {
                extractLocalityContractOf(expression)
            }
        }

    context(context: CheckerContext)
    fun resolveLocalityContractOf(expression: FirExpression): LocalityContract =
        cache.getValue(expression, context)

    context(context: CheckerContext)
    fun extractLocalityContractOf(expression: FirExpression): List<Locality>? =
        when (val expression = expression.unwrapExpression().removeCast()) {
            is FirPropertyAccessExpression ->
                (expression.calleeReference.symbol as? FirVariableSymbol)
                    ?.resolveLocalityContract()
            is FirAnonymousFunctionExpression ->
                expression.resolvedType.resolveLocalityContract(session)
            else -> expression.collectTails()
                .map { tail -> resolveLocalityContractOf(tail) }
                .reduceOrNull(LocalityContract::join)
        }
}

private val FirSession.expressionLocalityContractResolver: ExpressionLocalityContractResolver by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun FirExpression.resolveLocalityContract(): LocalityContract =
    context.session.expressionLocalityContractResolver.resolveLocalityContractOf(this)
