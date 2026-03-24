package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.formver.plugin.compiler.fir.receiver
import org.jetbrains.kotlin.formver.plugin.compiler.fir.tails
import java.util.WeakHashMap

class ConeLocalAttributeExtractor(
    private val cache: WeakHashMap<FirElement, ConeLocalAttribute?>
) {
    context(context : CheckerContext)
    fun extract(expression: FirExpression): ConeLocalAttribute? {
        return cache.computeIfAbsent(expression) {
            expression.tails.fold(null) { result, tail ->
                val receiver = tail.receiver

                if (receiver == null) {
                    tail.declaredLocalAttribute
                } else {
                    result.union(
                        tail.declaredLocalAttribute.union(receiver.resolvedLocalAttribute)
                    )
                }
            }
        }
    }
}