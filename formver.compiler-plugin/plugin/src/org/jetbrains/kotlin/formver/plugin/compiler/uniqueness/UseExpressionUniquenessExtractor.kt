/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.uniqueness

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

object UseExpressionUniquenessExtractor : PathValueExtractor<Uniqueness, UniquenessTrie?>() {
    fun extract(expression: FirExpression, typingEnvironment: UniquenessTrie): Uniqueness {
        return expression.accept(this, typingEnvironment)
    }

    override val empty = Uniqueness.Unique

    override fun Uniqueness.join(other: Uniqueness): Uniqueness {
        return join(other)
    }

    override fun visitReceiverExpression(
        symbol: FirBasedSymbol<*>?,
        explicitReceiver: FirExpression?,
        dispatchReceiver: FirExpression?,
        data: UniquenessTrie?
    ): Uniqueness {
        val nextData = symbol?.let { data?.get(it) }
        val receiverData = if (symbol != null) nextData else data
        val componentUniqueness = nextData?.element ?: Uniqueness.Shared
        val receiverUniqueness = explicitReceiver?.visit(receiverData)
            ?: dispatchReceiver?.visit(receiverData)
            ?: empty

        return receiverUniqueness.join(componentUniqueness)
    }

    @OptIn(SymbolInternals::class)
    override fun visitFunctionCall(
        functionCall: FirFunctionCall,
        data: UniquenessTrie?
    ): Uniqueness {
        val functionDeclaration = functionCall.calleeReference.symbol?.fir

        return functionDeclaration?.requiredUniqueness ?: Uniqueness.Shared
    }

    override fun visitLiteralExpression(
        literalExpression: FirLiteralExpression,
        data: UniquenessTrie?
    ): Uniqueness {
        return Uniqueness.Unique
    }
}

/**
 * Extracts the uniqueness of [this] expression in [typingEnvironment].
 */
fun FirExpression.extractUniqueness(typingEnvironment: UniquenessTrie): Uniqueness =
    UseExpressionUniquenessExtractor.extract(this, typingEnvironment)
