package org.jetbrains.kotlin.formver.plugin.compiler.analysis

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSession.Companion.sessionComponentAccessor
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import java.util.WeakHashMap

/**
 * Resolves variables declared in a declaration.
 */
class FirDeclarationVariablesResolver(
    session: FirSession,
    private val cache: WeakHashMap<FirDeclaration, Set<FirVariable>>
) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory = Factory { session ->
            FirDeclarationVariablesResolver(session, WeakHashMap())
        }

        private fun resolveValueParameters(declaration: FirDeclaration) : Sequence<FirVariable> {
            if (declaration !is FirFunction) return emptySequence()

            return declaration.valueParameters.asSequence()
        }

        private fun resolveLocalProperties(declaration: FirDeclaration) : Sequence<FirVariable> {
            if (declaration !is FirControlFlowGraphOwner) return emptySequence()
            val graph = declaration.controlFlowGraphReference?.controlFlowGraph ?: return emptySequence()

            return graph.nodes.asSequence().filterIsInstance<VariableDeclarationNode>().map { it.fir }
        }
    }

    fun resolve(declaration: FirDeclaration): Set<FirVariable>? {
        return cache.computeIfAbsent(declaration) {
            buildSet {
                addAll(resolveValueParameters(declaration))
                addAll(resolveLocalProperties(declaration))
            }
        }
    }
}

val FirSession.firDeclarationVariablesResolver: FirDeclarationVariablesResolver by sessionComponentAccessor()

context(context: CheckerContext)
val FirDeclaration.variables: Set<FirVariable>?
    get() = context.session.firDeclarationVariablesResolver.resolve(this)
