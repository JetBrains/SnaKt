/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirWrappedExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor

typealias Path = List<FirBasedSymbol<*>>

/**
 * A visitor for extracting an atomic receiving path from a [org.jetbrains.kotlin.fir.expressions.FirExpression].
 *
 * This visitor differs from [ValuePathCollector] in that it only extracts the path of a receiver expression in an
 * assignment, as opposed to the possible yielded paths.
 */
object ReceiverPathExtractor : FirVisitor<Path?, Unit>() {
    override fun visitElement(
        element: FirElement,
        data: Unit
    ): Path? = null

    override fun visitResolvedNamedReference(
        resolvedNamedReference: FirResolvedNamedReference,
        data: Unit
    ): Path = listOf(resolvedNamedReference.resolvedSymbol)

    override fun visitProperty(
        property: FirProperty,
        data: Unit
    ): Path = listOf(property.symbol)

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: Unit
    ): Path? {
        val parent = propertyAccessExpression.explicitReceiver?.accept(this, data) ?: emptyList()
        val callee = propertyAccessExpression.calleeReference.accept(this, data) ?: emptyList()

        if (parent.isEmpty() && callee.isEmpty()) {
            return null
        } else {
            return parent + callee
        }
    }
}

val FirElement.receiverPath
    get()  = accept(ReceiverPathExtractor, Unit)

/**
 * A visitor for collecting the paths that may be yielded by a [org.jetbrains.kotlin.fir.expressions.FirExpression].
 *
 * This visitor differs from [ReceiverPathExtractor] in that it collects all paths that may be yielded by an expression.
 * In the future this visitor will be expanded to handle more complex expressions such as `if` expressions, elvis
 * operator, etc.
 */
object ValuePathCollector : FirVisitor<List<Path>, Unit>() {
    override fun visitElement(
        element: FirElement,
        data: Unit
    ): List<Path> = emptyList()

    override fun visitResolvedNamedReference(
        resolvedNamedReference: FirResolvedNamedReference,
        data: Unit
    ): List<Path> = listOf(ReceiverPathExtractor.visitResolvedNamedReference(resolvedNamedReference, data))

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: Unit
    ): List<Path> {
        val path = ReceiverPathExtractor.visitPropertyAccessExpression(propertyAccessExpression, data)

        if (path != null) {
            return listOf(path)
        } else {
            return emptyList()
        }
    }

    override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: Unit): List<Path> {
        return smartCastExpression.originalExpression.accept(this, data)
    }

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Unit): List<Path> {
        return typeOperatorCall.argumentList.arguments.singleOrNull()?.accept(this, data) ?: emptyList()
    }
}

val FirElement.valuePaths
    get()  = accept(ValuePathCollector, Unit)
