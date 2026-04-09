package org.jetbrains.kotlin.formver.plugin.compiler.analysis

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSession.Companion.sessionComponentAccessor
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import java.util.WeakHashMap

class FirVariableDeclarationResolver(
    session: FirSession,
    private val cache: WeakHashMap<FirVariable, FirDeclaration>
) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory = Factory { session ->
            FirVariableDeclarationResolver(session, WeakHashMap())
        }
    }

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext)
    fun resolve(variable: FirVariable): FirDeclaration? {
        return cache.computeIfAbsent(variable) {
            for (option in context.containingDeclarations.asReversed()) {
                val declaration = option.fir

                if (declaration.variables?.contains(variable) == true) {
                    return@computeIfAbsent option.fir
                }
            }

            null
        }
    }
}

val FirSession.firVariableDeclarationResolver: FirVariableDeclarationResolver by sessionComponentAccessor()

context(context: CheckerContext)
val FirVariable.declaration: FirDeclaration?
    get() = context.session.firVariableDeclarationResolver.resolve(this)
