package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.formver.core.annotationId
import org.jetbrains.kotlin.formver.plugin.compiler.locality.FirDeclarationLocalityValueExtractor.isLocal

object FirDeclarationLocalityValueExtractor {
    private val localityAnnotationId = annotationId("Borrowed")

    val FirAnnotationContainer.isLocal
        get() = annotations.any { it.annotationTypeRef.coneType.classId == localityAnnotationId }

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
    private fun FirBasedSymbol<*>.resolveOwner(): FirDeclaration? {
        return when (this) {
            is FirReceiverParameterSymbol -> containingDeclarationSymbol.fir
            is FirValueParameterSymbol -> containingDeclarationSymbol.fir
            is FirPropertySymbol -> {
                context.containingDeclarations
                    .asReversed()
                    .firstOrNull { it.fir.declares(fir) }
                    ?.fir
            }
            else -> null
        }
    }

    context(context: CheckerContext)
    private fun FirDeclaration.resolve(): LocalityValue {
        if (annotations.none { it.annotationTypeRef.coneType.classId == localityAnnotationId }) {
            return LocalityValue.Global
        }

        return LocalityValue.Local(owner = symbol.resolveOwner())
    }

    context(context: CheckerContext)
    fun extract(declaration: FirDeclaration): LocalityValue {
        return declaration.resolve()
    }

}

context(context: CheckerContext)
val FirDeclaration.declaredLocality: LocalityValue
    get() = FirDeclarationLocalityValueExtractor.extract(this)

@OptIn(SymbolInternals::class)
context(context: CheckerContext)
val FirValueParameter.requiredLocality: LocalityValue
    get() {
        if (!isLocal) {
            return LocalityValue.Global
        }

        return LocalityValue.Local(
            owner = context.containingDeclarations.last().fir
        )
    }

@OptIn(SymbolInternals::class)
context(context: CheckerContext)
val FirReceiverParameter.requiredLocality: LocalityValue
    get() {
        if (!isLocal) {
            return LocalityValue.Global
        }

        return LocalityValue.Local(
            owner = context.containingDeclarations.last().fir
        )
    }
