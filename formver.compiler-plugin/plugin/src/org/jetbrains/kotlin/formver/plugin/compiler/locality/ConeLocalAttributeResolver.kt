package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSession.Companion.sessionComponentAccessor
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import java.util.WeakHashMap

class ConeLocalAttributeResolver(
    session: FirSession,
    val localAttributeExtractor: ConeLocalAttributeExtractor
) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory = Factory { session ->
            ConeLocalAttributeResolver(
                session,
                ConeLocalAttributeExtractor(WeakHashMap())
            )
        }
    }

    context(context : CheckerContext)
    fun resolve(expression: FirExpression): ConeLocalAttribute? =
        localAttributeExtractor.resolveLocalAttribute(expression)
}

val FirSession.coneLocalAttributeResolver: ConeLocalAttributeResolver by sessionComponentAccessor()

context(context : CheckerContext)
val FirExpression.localAttribute: ConeLocalAttribute?
    get() = context.session.coneLocalAttributeResolver.resolve(this)
