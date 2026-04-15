/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.uniqueness

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

/**
 * Extracts the typing environment after evaluating one expression.
 */
object UseUniquenessTrieExtractor : PathValueExtractor<UniquenessTrie, UniquenessTrie>() {
    fun extract(expression: FirExpression, typingEnvironment: UniquenessTrie): UniquenessTrie {
        val movedTerminals = expression.visit(typingEnvironment)

        return typingEnvironment.join(movedTerminals)
    }

    override val empty: UniquenessTrie = UniquenessTrie(Uniqueness.Unique)

    override fun UniquenessTrie.join(other: UniquenessTrie): UniquenessTrie {
        return join(other)
    }

    override fun visitReceiverExpression(
        symbol: FirBasedSymbol<*>?,
        explicitReceiver: FirExpression?,
        dispatchReceiver: FirExpression?,
        data: UniquenessTrie
    ): UniquenessTrie {
        if (symbol == null) {
            return explicitReceiver?.visit(data)
                ?: dispatchReceiver?.visit(data)
                ?: empty
        }

        return empty.copy(
            children = mapOf(symbol to UniquenessTrie(Uniqueness.Moved))
        )
    }
}

/**
 * Extracts the uniqueness trie after using [this] expression in [typingEnvironment].
 */
fun FirExpression.extractPostUseState(typingEnvironment: UniquenessTrie): UniquenessTrie =
    UseUniquenessTrieExtractor.extract(this, typingEnvironment)
