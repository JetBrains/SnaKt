package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.plugin.compiler.fir.firTails
import java.util.WeakHashMap

class ConeLocalAttributeExtractor(
    private val session: FirSession,
    private val cache: WeakHashMap<FirElement, ConeLocalAttribute?>
) : FirVisitor<ConeLocalAttribute?, Unit>() {
    private fun FirExpression.visit(): ConeLocalAttribute? {
        return accept(this@ConeLocalAttributeExtractor, Unit)
    }

    fun extractLocalAttribute(element: FirElement): ConeLocalAttribute? {
        val firTails = session.firTails

        return cache.computeIfAbsent(element) {
            firTails[element].fold(null) { result, next ->
                result?.union(next.visit())
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