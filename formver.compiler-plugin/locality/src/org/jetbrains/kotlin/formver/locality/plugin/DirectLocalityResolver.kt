/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor

object ImmediateLocalityExtractor : FirVisitor<Locality?, CheckerContext>() {
    override fun visitElement(element: FirElement, data: CheckerContext): Locality? {
        return null
    }

    @OptIn(SymbolInternals::class)
    override fun visitThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: CheckerContext
    ): Locality? {
        val boundSymbol = thisReceiverExpression.calleeReference.boundSymbol

        return when (boundSymbol) {
            is FirReceiverParameterSymbol -> boundSymbol.fir.extractActualLocality()
            else -> null
        }
    }

    @OptIn(SymbolInternals::class)
    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: CheckerContext
    ): Locality? {
        val calleeSymbol = propertyAccessExpression.calleeReference.symbol

        return when (calleeSymbol) {
            is FirVariableSymbol<*> ->
                when (val declaration = calleeSymbol.fir) {
                    is FirValueParameter ->
                        declaration.extractActualLocality()

                    is FirProperty ->
                        with(data) {
                            declaration.extractLocality()
                        }

                    else -> null
                }

            else -> null
        }
    }
}

context(context: CheckerContext)
fun FirExpression.resolveDirectLocality(): Locality? {
    return unwrapExpression().accept(ImmediateLocalityExtractor, context)
}
