package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSession.Companion.sessionComponentAccessor
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import java.util.WeakHashMap

class ConeLocalAttributeProvider(
    session: FirSession,
    val localAttributeExtractor: ConeLocalAttributeExtractor
) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory = Factory { session ->
            ConeLocalAttributeProvider(
                session,
                ConeLocalAttributeExtractor(
                    session,
                    WeakHashMap()
                )
            )
        }
    }
    
    operator fun get(element: FirElement): ConeLocalAttribute? =
        localAttributeExtractor.extractLocalAttribute(element)
}

val FirSession.coneLocalAttributes: ConeLocalAttributeProvider by sessionComponentAccessor()

context(context : CheckerContext)
val FirElement.localAttribute: ConeLocalAttribute?
    get() = context.session.coneLocalAttributes[this]
