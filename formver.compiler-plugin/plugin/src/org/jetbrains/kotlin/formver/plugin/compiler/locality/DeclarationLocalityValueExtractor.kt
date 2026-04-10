package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.formver.core.annotationId

abstract class DeclarationLocalityValueExtractor {
    val localityAnnotationId = annotationId("Borrowed")

    val FirDeclaration.hasLocalityAnnotation: Boolean
        get() = annotations.any { it.annotationTypeRef.coneType.classId == localityAnnotationId }

    context(_: CheckerContext)
    abstract val FirDeclaration.owner: FirDeclaration?

    context(context: CheckerContext)
    fun extract(declaration: FirDeclaration): LocalityValue =
        if (declaration.hasLocalityAnnotation) {
            return LocalityValue.Local(declaration.owner)
        } else {
            return LocalityValue.Global
        }
}

@OptIn(SymbolInternals::class)
context(context: CheckerContext)
private fun FirProperty.resolveOwner(): FirDeclaration? {
    return context.containingDeclarations.reversed()
        .filter { declarationSymbol ->
            val declaration = declarationSymbol.fir

            if (declaration is FirControlFlowGraphOwner) {
                declaration.controlFlowGraphReference?.controlFlowGraph?.nodes?.any { node ->
                    node is VariableDeclarationNode && node.fir == this
                } ?: false
            } else {
                false
            }
        }
        .map {
            it.fir
        }
        .first()
}

private class UseLocalityValueExtractor : DeclarationLocalityValueExtractor() {
    @OptIn(SymbolInternals::class)
    context(_: CheckerContext)
    override val FirDeclaration.owner: FirDeclaration?
        get() = when (this) {
            is FirValueParameter -> containingDeclarationSymbol.fir
            is FirReceiverParameter -> containingDeclarationSymbol.fir
            is FirProperty -> resolveOwner()
            else -> null
        }
}

context(_: CheckerContext)
val FirDeclaration.actualLocality: LocalityValue
    get() = UseLocalityValueExtractor().extract(this)

private class DefinitionLocalityValueExtractor : DeclarationLocalityValueExtractor() {
    @OptIn(SymbolInternals::class)
    context(context: CheckerContext)
    override val FirDeclaration.owner: FirDeclaration?
        get() =  when (this) {
            is FirValueParameter -> context.containingDeclarations.lastOrNull()?.fir
            is FirReceiverParameter ->  context.containingDeclarations.lastOrNull()?.fir
            is FirProperty -> resolveOwner()
            else -> null
        }
}

context(_: CheckerContext)
val FirDeclaration.requiredLocality : LocalityValue
    get() = DefinitionLocalityValueExtractor().extract(this)
