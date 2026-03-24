package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.plugin.compiler.fir.tails
import java.util.WeakHashMap

class ConeLocalAttributeExtractor(
    private val cache: WeakHashMap<FirElement, ConeLocalAttribute?>
) : FirVisitor<ConeLocalAttribute?, Unit>() {
    private fun FirElement.visit(): ConeLocalAttribute? {
        return cache.computeIfAbsent(this) {
            accept(this@ConeLocalAttributeExtractor, Unit)
        }
    }

    context(context : CheckerContext)
    fun extract(expression: FirExpression): ConeLocalAttribute? {
        return cache.computeIfAbsent(expression) {
            expression.tails.fold(null) { result, next ->
                result.union(next.visit())
            }
        }
    }

    override fun visitElement(
        element: FirElement,
        data: Unit
    ): ConeLocalAttribute? {
        return when (element) {
            is FirExpression -> element.resolvedType.localAttribute
            else -> null
        }
    }
}