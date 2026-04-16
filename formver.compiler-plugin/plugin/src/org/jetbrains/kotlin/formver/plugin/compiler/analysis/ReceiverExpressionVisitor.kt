/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.analysis

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

abstract class ReceiverExpressionVisitor<T, D> : TailExpressionVisitor<T, D>() {
    abstract fun visitReceiverExpression(
        symbol: FirBasedSymbol<*>?,
        explicitReceiver: FirExpression?,
        dispatchReceiver: FirExpression?,
        data: D
    ): T

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: D
    ): T {
        return visitQualifiedAccessExpression(propertyAccessExpression, data)
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: D
    ): T {
        return visitReceiverExpression(
            symbol = qualifiedAccessExpression.calleeReference.symbol,
            explicitReceiver = qualifiedAccessExpression.explicitReceiver,
            dispatchReceiver = qualifiedAccessExpression.dispatchReceiver,
            data = data
        )
    }

    override fun visitSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: D
    ): T {
        val selectorSymbol = (safeCallExpression.selector as? FirQualifiedAccessExpression)?.calleeReference?.symbol

        return visitReceiverExpression(
            symbol = selectorSymbol,
            explicitReceiver = safeCallExpression.receiver,
            dispatchReceiver = null,
            data = data
        )
    }
}