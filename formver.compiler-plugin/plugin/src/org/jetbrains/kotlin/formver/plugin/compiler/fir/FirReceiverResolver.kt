package org.jetbrains.kotlin.formver.plugin.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSession.Companion.sessionComponentAccessor
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import java.util.WeakHashMap

class FirReceiverResolver(
    session: FirSession,
    private val receiverExtractor: FirReceiverExtractor,
) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory = Factory { session ->
            FirReceiverResolver(
                session,
                FirReceiverExtractor(WeakHashMap())
            )
        }
    }
    
    fun resolve(expression: FirExpression): FirExpression? =
        receiverExtractor.extract(expression)
}

val FirSession.firReceiversResolver: FirReceiverResolver by sessionComponentAccessor()

context(context : CheckerContext)
val FirExpression.receiver: FirExpression?
    get() = context.session.firReceiversResolver.resolve(this)
