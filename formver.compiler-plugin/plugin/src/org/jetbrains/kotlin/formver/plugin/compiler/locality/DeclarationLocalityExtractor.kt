package org.jetbrains.kotlin.formver.plugin.compiler.locality

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
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.formver.core.annotationId

/**
 * Extracts the locality of a declaration.
 */
abstract class DeclarationLocalityExtractor {
    companion object {
        val localityAnnotationId = annotationId("Borrowed")

        val FirDeclaration.hasLocalityAnnotation: Boolean
            get() = annotations.any { it.annotationTypeRef.coneType.classId == localityAnnotationId }
    }

    context(_: CheckerContext)
    abstract val FirDeclaration.owner: FirBasedSymbol<*>?

    context(context: CheckerContext)
    fun extract(declaration: FirDeclaration): Locality =
        if (declaration.hasLocalityAnnotation) {
            return Locality.Local(declaration.owner)
        } else {
            return Locality.Global
        }
}

private fun FirControlFlowGraphOwner.declares(property: FirProperty): Boolean =
    controlFlowGraphReference?.controlFlowGraph?.nodes?.any { node ->
        node is VariableDeclarationNode && node.fir == property
    } ?: false

@OptIn(SymbolInternals::class)
context(context: CheckerContext)
private fun FirProperty.resolveOwner(): FirBasedSymbol<*>? =
    context.containingDeclarations
        .asReversed()
        .firstOrNull { declarationSymbol ->
            val declaration = declarationSymbol.fir as? FirControlFlowGraphOwner ?: return@firstOrNull false
            declaration.declares(this)
        }

private class UseLocalityExtractor : DeclarationLocalityExtractor() {
    context(_: CheckerContext)
    override val FirDeclaration.owner: FirBasedSymbol<*>?
        get() = when (this) {
            is FirValueParameter -> containingDeclarationSymbol
            is FirReceiverParameter -> containingDeclarationSymbol
            is FirProperty -> resolveOwner()
            else -> null
        }
}

context(_: CheckerContext)
val FirDeclaration.usageLocality: Locality
    get() = UseLocalityExtractor().extract(this)

private val CheckerContext.currentDeclaration: FirBasedSymbol<*>?
    get() = containingDeclarations.lastOrNull()

private class DefinitionLocalityExtractor : DeclarationLocalityExtractor() {
    context(context: CheckerContext)
    override val FirDeclaration.owner: FirBasedSymbol<*>?
        get() =  when (this) {
            is FirValueParameter -> context.currentDeclaration
            is FirReceiverParameter ->  context.currentDeclaration
            is FirProperty -> resolveOwner()
            else -> null
        }
}

context(_: CheckerContext)
val FirDeclaration.requiredLocality : Locality
    get() = DefinitionLocalityExtractor().extract(this)
