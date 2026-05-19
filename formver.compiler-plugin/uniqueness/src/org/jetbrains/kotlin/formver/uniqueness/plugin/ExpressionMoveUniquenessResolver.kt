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
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.type.plugin.ExpressionTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.UnifyingExpressionTypeResolver

object TerminalMoveUniquenessResolver : ExpressionTypeResolver<UniquenessTrie> {
    context(context: CheckerContext)
    override fun resolveTypeOf(expression: FirExpression): UniquenessTrie {
        return when (expression) {
            is FirQualifiedAccessExpression ->
                when (val symbol = expression.calleeReference.symbol) {
                    is FirVariableSymbol -> symbol
                    is FirReceiverParameterSymbol -> symbol.resolveLocality()
                    else -> null
                }

            else -> null
        }
    }
}

class ExpressionMoveUniquenessResolver(session: FirSession) :
        ExpressionTypeResolver<UniquenessTrie> by UnifyingExpressionTypeResolver(
            session.firCachesFactory,
            TerminalMoveUniquenessResolver,
            UniquenessTrieUnifier
        )

fun FirExpression.resolveMoveUniqueness(): UniquenessTrie {
    TODO("Not yet implemented")
}
