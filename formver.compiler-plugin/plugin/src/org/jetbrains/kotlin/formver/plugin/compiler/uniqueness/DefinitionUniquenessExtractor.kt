/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.uniqueness

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

/**
 * Extracts expression uniqueness from a definition expression.
 */
object DefinitionUniquenessExtractor : PathValueExtractor<Uniqueness, Unit>() {
    fun extract(expression: FirExpression): Uniqueness {
        return expression.accept(this, Unit)
    }

    override val empty = Uniqueness.Unique

    override fun Uniqueness.join(other: Uniqueness): Uniqueness {
        return join(other)
    }

    @OptIn(SymbolInternals::class)
    override fun visitReceiverExpression(
        symbol: FirBasedSymbol<*>?,
        explicitReceiver: FirExpression?,
        dispatchReceiver: FirExpression?,
        data: Unit
    ): Uniqueness {
        val componentUniqueness = symbol?.fir?.requiredUniqueness ?: Uniqueness.Shared
        val receiverUniqueness = explicitReceiver?.visit(data)
            ?: dispatchReceiver?.visit(data)
            ?: empty

        return receiverUniqueness.join(componentUniqueness)
    }
}

/**
 * Extracts the uniqueness required by [this] expression definition.
 */
val FirExpression.requiredUniqueness: Uniqueness
    get() = DefinitionUniquenessExtractor.extract(this)

