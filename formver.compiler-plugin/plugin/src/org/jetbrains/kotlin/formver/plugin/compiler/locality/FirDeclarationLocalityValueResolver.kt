package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.coneType

object FirDeclarationLocalityAttributeExtractor {
    private fun FirDeclaration.declares(property: FirProperty): Boolean {
        if (this !is FirControlFlowGraphOwner) {
            return false
        }

        val graph = controlFlowGraphReference?.controlFlowGraph ?: return false

        return graph.nodes.asSequence()
            .filterIsInstance<VariableDeclarationNode>()
            .any { it.fir == property }
    }

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext)
    private fun resolveOwner(property: FirProperty): FirDeclaration? {
        return context.containingDeclarations
            .asReversed()
            .firstOrNull { it.fir.declares(property) }
            ?.fir
    }

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext)
    private fun resolveOwner(symbol: FirBasedSymbol<*>): FirDeclaration? {
        return when (symbol) {
            is FirReceiverParameterSymbol -> context.containingDeclarations.lastOrNull()?.fir
            is FirValueParameterSymbol -> context.containingDeclarations.lastOrNull()?.fir
            is FirPropertySymbol -> resolveOwner(symbol.fir)
            else -> null
        }
    }

    context(context: CheckerContext)
    fun extract(variable: FirVariable): ConeLocalityAttribute? {
        return variable.returnTypeRef.coneType.localAttribute?.copy(
            owner = resolveOwner(variable.symbol)
        )
    }

    context(context: CheckerContext)
    fun extract(variable: FirReceiverParameter): ConeLocalityAttribute? {
        return variable.typeRef.coneType.localAttribute?.copy(
            owner = resolveOwner(variable.symbol)
        )
    }

}

context(context: CheckerContext)
val FirVariable.localityAttribute: ConeLocalityAttribute?
    get() = FirDeclarationLocalityAttributeExtractor.extract(this)

context(context: CheckerContext)
val FirReceiverParameter.localityAttribute: ConeLocalityAttribute?
    get() = FirDeclarationLocalityAttributeExtractor.extract(this)
