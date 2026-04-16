/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.uniqueness

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.PathValueExtractor
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.copy

abstract class ExpressionUniquenessStateExtractor : PathValueExtractor<UniquenessState, UniquenessState>() {
    override val empty: UniquenessState = UniquenessState(Uniqueness.Shared)

    override fun UniquenessState.join(other: UniquenessState): UniquenessState {
        return join(other)
    }
}

/**
 * Extracts the uniqueness state after evaluating an expression.
 */
object UseUniquenessStateExtractor : ExpressionUniquenessStateExtractor() {
    fun extract(expression: FirExpression, typingEnvironment: UniquenessState): UniquenessState {
        val movedTerminals = expression.visit(typingEnvironment)

        return typingEnvironment.join(movedTerminals)
    }

    override fun visitReceiverExpression(
        symbol: FirBasedSymbol<*>?,
        explicitReceiver: FirExpression?,
        dispatchReceiver: FirExpression?,
        data: UniquenessState
    ): UniquenessState {
        if (symbol == null) {
            return explicitReceiver?.visit(data)
                ?: dispatchReceiver?.visit(data)
                ?: empty
        }

        return empty.copy(
            children = mapOf(symbol to UniquenessState(Uniqueness.Moved))
        )
    }
}

/**
 * Extracts the uniqueness state after using [this] expression in [typingEnvironment].
 */
fun FirExpression.extractPostUseState(typingEnvironment: UniquenessState): UniquenessState =
    UseUniquenessStateExtractor.extract(this, typingEnvironment)


/**
 * Extracts the uniqueness state after defining an expression.
 */
object DefinitionUniquenessStateExtractor : ExpressionUniquenessStateExtractor() {
    fun extract(expression: FirExpression, typingEnvironment: UniquenessState): UniquenessState {
        return expression.visit(typingEnvironment)
    }

    override val empty: UniquenessState = UniquenessState(Uniqueness.Unique)

    override fun UniquenessState.join(other: UniquenessState): UniquenessState {
        return join(other)
    }

    @OptIn(SymbolInternals::class)
    override fun visitReceiverExpression(
        symbol: FirBasedSymbol<*>?,
        explicitReceiver: FirExpression?,
        dispatchReceiver: FirExpression?,
        data: UniquenessState
    ): UniquenessState {
        val symbol = symbol ?: return data
        val uniqueness = symbol.fir.requiredUniqueness
        val receiverData = data[symbol] ?: empty
        val receiverEnvironment = explicitReceiver?.visit(receiverData)
            ?: dispatchReceiver?.visit(receiverData)
            ?: empty

        return data.copy(children = data.children + (symbol to receiverEnvironment.copy(element = uniqueness)))
    }
}

/**
 * Extracts the uniqueness trie after defining [this] expression in [typingEnvironment].
 */
fun FirExpression.extractPostDefinitionState(typingEnvironment: UniquenessState): UniquenessState =
    DefinitionUniquenessStateExtractor.extract(this, typingEnvironment)