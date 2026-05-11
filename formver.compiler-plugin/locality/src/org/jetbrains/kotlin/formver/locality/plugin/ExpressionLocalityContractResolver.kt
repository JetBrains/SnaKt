/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.valueParameterTypesIncludingReceiver

class ExpressionLocalityContractResolver(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> ExpressionLocalityContractResolver(session) }
        }
    }

    private val cacheFactory = session.firCachesFactory

    @OptIn(SymbolInternals::class)
    private fun extractLocalityContractOf(expression: FirExpression): List<LocalityAttribute?>? {
        val normalizedExpression = expression.unwrapExpression().unwrapCast()
        val tailContract = normalizedExpression.collectTails()
            .filter { it !== normalizedExpression }
            .map { resolveLocalityContractOf(it) }
            .unionLocalityContracts()

        if (tailContract != null) return tailContract

        return when (normalizedExpression) {
            is FirPropertyAccessExpression -> {
                val property = (normalizedExpression.calleeReference.symbol as? FirVariableSymbol<*>)?.fir as? FirProperty
                property?.initializer?.let { resolveLocalityContractOf(it) }
                    ?: normalizedExpression.resolvedType.resolveLocalityContract()
            }
            else ->
                normalizedExpression.resolvedType.resolveLocalityContract()
        }
    }

    private val cache =
        cacheFactory.createCache { expression: FirExpression, _: Unit ->
            extractLocalityContractOf(expression)
        }

    fun resolveLocalityContractOf(expression: FirExpression): List<LocalityAttribute?>? =
        cache.getValue(expression, Unit)

    private fun ConeKotlinType.resolveLocalityContract(): List<LocalityAttribute?>? {
        if (!isSomeFunctionType(session)) return null

        val functionType = lowerBoundIfFlexible() as? ConeClassLikeType ?: return null

        return functionType.valueParameterTypesIncludingReceiver(session).map { it.locality }
    }
}

private val FirSession.expressionLocalityContractResolver: ExpressionLocalityContractResolver by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun FirExpression.resolveLocalityContract(): List<LocalityAttribute?>? =
    context.session.expressionLocalityContractResolver.resolveLocalityContractOf(this)

// TODO: Get rid of this
private fun Sequence<List<LocalityAttribute?>?>.unionLocalityContracts(): List<LocalityAttribute?>? {
    val contracts = filterNotNull().toList()
    val arity = contracts.firstOrNull()?.size ?: return null

    if (contracts.any { it.size != arity }) return null

    return (0 until arity).map { index ->
        if (contracts.any { it[index] != null }) LocalityAttribute else null
    }
}
