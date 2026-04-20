/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.locality

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
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.ExpressionPathFolder

/**
 * Extracts the locality of an expression with respect to the outer declarations specified in [context].
 */
class ExpressionLocalityExtractor(
    private val context: CheckerContext
) : ExpressionPathFolder<Locality, Unit>() {
    override val empty = Locality.Global

    override fun Locality.join(other: Locality): Locality {
        return join(other)
    }

    @OptIn(SymbolInternals::class)
    fun FirBasedSymbol<*>.extractLocality(): Locality {
        return when (this) {
            is FirVariableSymbol<*> ->
                when (val declaration = fir) {
                    is FirValueParameter ->
                        declaration.extractActualLocality()
                    is FirProperty ->
                        context(context) {
                            declaration.extractLocality()
                        }
                    else -> Locality.Global
                }
            else -> Locality.Global
        }
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Unit
    ): Locality {
        return qualifiedAccessExpression.explicitReceiver?.visit(data)
            ?: qualifiedAccessExpression.dispatchReceiver?.visit(data)
            ?: qualifiedAccessExpression.calleeReference.symbol?.extractLocality() ?: empty
    }

    @OptIn(SymbolInternals::class)
    override fun visitThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: Unit
    ): Locality {
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
fun FirExpression.extractLocality(): Locality {
    return accept(ExpressionLocalityExtractor(context), Unit)
}
