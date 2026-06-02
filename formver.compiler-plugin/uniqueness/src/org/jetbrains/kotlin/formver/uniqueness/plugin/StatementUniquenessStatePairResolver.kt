package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

class StatementUniquenessStatePairResolver(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> StatementUniquenessStatePairResolver(session) }
        }

        context(context: CheckerContext)
        fun resolveUniquenessStatePairOf(statement: FirStatement): UniquenessStatePair? =
            context.session.statementUniquenessStatePairResolver.resolveUniquenessStatePairOf(statement)
    }

    context(context: CheckerContext)
    fun resolveUniquenessStatePairOf(statement: FirStatement): UniquenessStatePair? {
        val functionSymbol = context.findClosest<FirFunctionSymbol<*>>()
        val graph = functionSymbol?.resolvedControlFlowGraphReference?.controlFlowGraph

        return graph?.resolveUniquenessStateMapping()[statement]
    }
}

private val FirSession.statementUniquenessStatePairResolver: StatementUniquenessStatePairResolver
        by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun FirExpression.resolveInputUniquenessState(): UniquenessState? =
    StatementUniquenessStatePairResolver.resolveUniquenessStatePairOf(this)?.first

context(context: CheckerContext)
fun FirExpression.resolveOutputUniquenessState(): UniquenessState? =
    StatementUniquenessStatePairResolver.resolveUniquenessStatePairOf(this)?.second
