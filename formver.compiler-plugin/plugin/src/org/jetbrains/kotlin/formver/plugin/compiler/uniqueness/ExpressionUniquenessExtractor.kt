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
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.ReceiverExpressionVisitor

/**
 * Common building blocks to extract expression uniqueness.
 *
 * [D] is the visitor data type threaded through the expression traversal.
 */
abstract class ExpressionUniquenessExtractor<D> : ReceiverExpressionVisitor<Uniqueness, D>() {
    override val empty = Uniqueness.Shared

    override fun Uniqueness.join(other: Uniqueness): Uniqueness {
        return join(other)
    }
}

/**
 * Extracts expression uniqueness from a definition expression.
 */
object DefinitionUniquenessExtractor : ExpressionUniquenessExtractor<Unit>() {
    fun extract(expression: FirExpression): Uniqueness {
        return expression.accept(this, Unit)
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

object UseUniquenessExtractor : ExpressionUniquenessExtractor<UniquenessState?>() {
    fun extract(expression: FirExpression, typingEnvironment: UniquenessState): Uniqueness {
        return expression.accept(this, typingEnvironment)
    }

    override fun visitReceiverExpression(
        symbol: FirBasedSymbol<*>?,
        explicitReceiver: FirExpression?,
        dispatchReceiver: FirExpression?,
        data: UniquenessState?
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
        data: UniquenessState?
    ): Uniqueness {
        val functionDeclaration = functionCall.calleeReference.symbol?.fir

        return functionDeclaration?.requiredUniqueness ?: Uniqueness.Shared
    }

    override fun visitLiteralExpression(
        literalExpression: FirLiteralExpression,
        data: UniquenessState?
    ): Uniqueness {
        return Uniqueness.Unique
    }
}

/**
 * Extracts the uniqueness of [this] expression in [typingEnvironment].
 */
fun FirExpression.extractUniqueness(typingEnvironment: UniquenessState): Uniqueness =
    UseUniquenessExtractor.extract(this, typingEnvironment)
