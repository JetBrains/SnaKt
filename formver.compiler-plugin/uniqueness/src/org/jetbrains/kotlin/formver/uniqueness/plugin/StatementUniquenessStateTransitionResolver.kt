package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

private fun FirStatement.normalize(): FirStatement =
    when (this) {
        is FirExpression -> unwrapExpression()
        else -> this
    }

class StatementUniquenessStateTransitionResolver(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> StatementUniquenessStateTransitionResolver(session) }
        }

        context(context: CheckerContext)
        fun resolveUniquenessStateTransitionOf(statement: FirStatement): UniquenessStateTransition? =
            context.session.statementUniquenessStateTransitionResolver.resolveUniquenessStateTransitionOf(statement)
    }

    context(context: CheckerContext)
    fun resolveUniquenessStateTransitionOf(statement: FirStatement): UniquenessStateTransition? {
        val functionSymbol = context.findClosest<FirFunctionSymbol<*>>()
        val graph = functionSymbol?.resolvedControlFlowGraphReference?.controlFlowGraph

        return graph?.resolveUniquenessStateTransitions()[statement.normalize()]
    }
}

private val FirSession.statementUniquenessStateTransitionResolver: StatementUniquenessStateTransitionResolver
        by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun FirStatement.resolveUniquenessStateTransition(): UniquenessStateTransition? =
    StatementUniquenessStateTransitionResolver.resolveUniquenessStateTransitionOf(this)

context(context: CheckerContext)
fun FirStatement.resolveInputUniquenessState(): UniquenessState? =
    resolveUniquenessStateTransition()?.first

context(context: CheckerContext)
fun FirStatement.resolveOutputUniquenessState(): UniquenessState? =
    resolveUniquenessStateTransition()?.second
