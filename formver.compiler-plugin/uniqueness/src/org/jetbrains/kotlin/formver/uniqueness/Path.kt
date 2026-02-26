package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor

typealias Path = List<FirBasedSymbol<*>>

object PathExtractor : FirVisitor<Path?, Unit>() {

    override fun visitElement(element: FirElement, data: Unit) = null

    override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference, data: Unit): Path =
        listOf(resolvedNamedReference.resolvedSymbol)

    override fun visitProperty(property: FirProperty, data: Unit): Path =
        listOf(property.symbol)

    override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: Unit): Path {
        val parent = propertyAccessExpression.explicitReceiver?.accept(this, data) ?: emptyList()
        val callee = propertyAccessExpression.calleeReference.accept(this, data) ?: emptyList()

        return parent + callee
    }

}

/**
 * Attempts to convert a FirElement to a Path.
 *
 * @return the path representation of the FirElement, or `null` if conversion is not possible.
 */
fun FirElement.toPath() =
    this.accept(PathExtractor, Unit)