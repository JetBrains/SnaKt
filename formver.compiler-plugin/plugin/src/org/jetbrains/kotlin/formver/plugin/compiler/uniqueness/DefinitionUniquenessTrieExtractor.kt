/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.uniqueness

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

object DefinitionUniquenessTrieExtractor : PathValueExtractor<UniquenessTrie, UniquenessTrie>() {
    fun extract(expression: FirExpression, typingEnvironment: UniquenessTrie): UniquenessTrie {
        return expression.visit(typingEnvironment)
    }

    override val empty: UniquenessTrie = UniquenessTrie(Uniqueness.Unique)

    override fun UniquenessTrie.join(other: UniquenessTrie): UniquenessTrie {
        return join(other)
    }

    @OptIn(SymbolInternals::class)
    override fun visitReceiverExpression(
        symbol: FirBasedSymbol<*>?,
        explicitReceiver: FirExpression?,
        dispatchReceiver: FirExpression?,
        data: UniquenessTrie
    ): UniquenessTrie {
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
fun FirExpression.extractPostDefinitionState(typingEnvironment: UniquenessTrie): UniquenessTrie =
    DefinitionUniquenessTrieExtractor.extract(this, typingEnvironment)
