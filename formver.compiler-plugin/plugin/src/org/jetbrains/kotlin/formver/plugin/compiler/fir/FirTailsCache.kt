package org.jetbrains.kotlin.formver.plugin.compiler.fir

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSession.Companion.sessionComponentAccessor
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.expressions.FirExpression
import java.util.WeakHashMap

class TailsCache(
    session: FirSession,
    cache: WeakHashMap<FirElement, Sequence<FirExpression>>
) : FirExtensionSessionComponent(session), MutableMap<FirElement, Sequence<FirExpression>> by cache {
    companion object {
        fun getFactory(): Factory = Factory { session ->
            TailsCache(session, WeakHashMap())
        }
    }
}

val FirSession.firTailsCache: TailsCache by sessionComponentAccessor()
