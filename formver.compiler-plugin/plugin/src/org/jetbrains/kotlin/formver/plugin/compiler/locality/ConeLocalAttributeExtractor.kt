package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.plugin.compiler.fir.tails
import java.util.WeakHashMap

class ConeLocalAttributeExtractor(
    private val cache: WeakHashMap<FirElement, ConeLocalAttribute?>
) : FirVisitor<ConeLocalAttribute?, Unit>() {
    private fun FirExpression.visit(): ConeLocalAttribute? {
        return accept(this@ConeLocalAttributeExtractor, Unit)
    }

    context(context : CheckerContext)
    fun extractLocalAttribute(element: FirElement): ConeLocalAttribute? {
        return cache.computeIfAbsent(element) {
            element.tails.fold(null) { result, next ->
                result.union(next.visit())
            }
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