package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.plugin.compiler.fir.FirTailsExtractor

class ConeLocalAttributeExtractor(
    val tailsExtractor: FirTailsExtractor
) : FirVisitor<ConeLocalAttribute?, Unit>() {
    private fun FirExpression.visit(): ConeLocalAttribute? {
        return accept(this@ConeLocalAttributeExtractor, Unit)
    }

    fun extract(element: FirElement): ConeLocalAttribute? {
        return tailsExtractor.extract(element).fold(null) { result, next ->
            result?.union(next.visit())
        }
    }

    override fun visitElement(
        element: FirElement,
        data: Unit
    ): ConeLocalAttribute? {
        return (element as? FirExpression)?.resolvedType?.localAttribute
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Unit
    ): ConeLocalAttribute? {
        return qualifiedAccessExpression.explicitReceiver
            ?.visit()
            ?.union(qualifiedAccessExpression.resolvedType.localAttribute)
    }
}