package org.jetbrains.kotlin.formver.plugin.compiler.fir

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSession.Companion.sessionComponentAccessor
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import java.util.WeakHashMap

class FirTailsProvider(
    session: FirSession,
    val tailsExtractor: FirTailsExtractor,
) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory = Factory { session ->
            FirTailsProvider(
                session,
                FirTailsExtractor(WeakHashMap())
            )
        }
    }
    
    operator fun get(element: FirElement): Sequence<FirExpression> =
        tailsExtractor.extractTails(element)
}

val FirSession.firTails: FirTailsProvider by sessionComponentAccessor()
