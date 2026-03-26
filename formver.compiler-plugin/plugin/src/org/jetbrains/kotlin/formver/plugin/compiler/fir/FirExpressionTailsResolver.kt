package org.jetbrains.kotlin.formver.plugin.compiler.fir

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSession.Companion.sessionComponentAccessor
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import java.util.WeakHashMap

class FirTailsResolver(
    session: FirSession,
    private val tailsExtractor: FirExpressionTailsExtractor,
) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory = Factory { session ->
            FirTailsResolver(
                session,
                FirExpressionTailsExtractor(WeakHashMap())
            )
        }
    }
    
    fun resolve(element: FirElement): Sequence<FirExpression> =
        tailsExtractor.extract(element)
}

val FirSession.firTailsResolver: FirTailsResolver by sessionComponentAccessor()

context(context : CheckerContext)
val FirExpression.tails: Sequence<FirExpression>
    get() = context.session.firTailsResolver.resolve(this)
