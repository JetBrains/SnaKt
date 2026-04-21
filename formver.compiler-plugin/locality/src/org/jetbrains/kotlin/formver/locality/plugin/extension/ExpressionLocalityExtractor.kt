/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin.extension

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.analysis.ExpressionPathFolder

/**
 * Extracts the locality of an expression with respect to the outer declarations specified in [context].
 */
class ExpressionLocalityExtractor(
    private val context: CheckerContext
) : ExpressionPathFolder<LocalityAttribute?, Unit>() {
    override val empty: LocalityAttribute? = null

    override fun LocalityAttribute?.join(other: LocalityAttribute?): LocalityAttribute? {
        return union(other)
    }

    @OptIn(SymbolInternals::class)
    fun FirBasedSymbol<*>.extractLocality(): LocalityAttribute? {
        return when (this) {
            is FirVariableSymbol<*> ->
                when (val declaration = fir) {
                    is FirValueParameter ->
                        declaration.extractActualLocality()
                    is FirProperty ->
                        context(context) {
                            declaration.extractLocality()
                        }
                    else -> null
                }
            else -> null
        }
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Unit
    ): LocalityAttribute? {
        val receiver = qualifiedAccessExpression.explicitReceiver
            ?: qualifiedAccessExpression.dispatchReceiver

        return if (receiver != null) {
            empty
        } else {
            qualifiedAccessExpression.calleeReference.symbol?.extractLocality() ?: empty
        }
    }

    @OptIn(SymbolInternals::class)
    override fun visitThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: Unit
    ): LocalityAttribute? {
        val boundSymbol = thisReceiverExpression.calleeReference.boundSymbol

        return when (boundSymbol) {
            is FirReceiverParameterSymbol -> boundSymbol.fir.extractActualLocality()
            else -> empty
        }
    }
}

/**
 * Extracts the locality of [this] expression with respect to the outer declarations.
 */
context(context: CheckerContext)
fun FirExpression.extractLocality(): LocalityAttribute? {
    return accept(ExpressionLocalityExtractor(context), Unit)
}
