/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
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
            Locality.Local(declaration.owner)
        } else {
            Locality.Global
        }
}

private fun FirControlFlowGraphOwner.declares(property: FirProperty): Boolean =
    controlFlowGraphReference?.controlFlowGraph?.nodes?.any { node ->
        node is VariableDeclarationNode && node.fir == property
    } ?: false

@OptIn(SymbolInternals::class)
context(context: CheckerContext)
private fun FirProperty.resolveOwner(): FirBasedSymbol<*>? =
    context.findClosest { declarationSymbol ->
        val declaration = declarationSymbol.fir as? FirControlFlowGraphOwner ?: return@findClosest false
        declaration.declares(this)
    }

private object ActualLocalityExtractor : DeclarationLocalityExtractor() {
    context(_: CheckerContext)
    override val FirDeclaration.owner: FirBasedSymbol<*>?
        get() = when (this) {
            is FirValueParameter -> containingDeclarationSymbol
            is FirReceiverParameter -> containingDeclarationSymbol
            is FirProperty -> resolveOwner()
            else -> null
        }
}

/**
 * Extracts the locality of [this] declaration usage with respect to the outer declarations.
 */
context(_: CheckerContext)
val FirDeclaration.actualLocality: Locality
    get() = ActualLocalityExtractor.extract(this)

private val CheckerContext.currentDeclaration: FirBasedSymbol<*>?
    get() = containingDeclarations.lastOrNull()

private object RequiredLocalityExtractor : DeclarationLocalityExtractor() {
    context(context: CheckerContext)
    override val FirDeclaration.owner: FirBasedSymbol<*>?
        get() =  when (this) {
            is FirValueParameter -> context.currentDeclaration
            is FirReceiverParameter ->  context.currentDeclaration
            is FirProperty -> resolveOwner()
            else -> null
        }
}

/**
 * Extracts the locality required by [this] declaration with respect to the outer declarations.
 */
context(_: CheckerContext)
val FirDeclaration.requiredLocality : Locality
    get() = RequiredLocalityExtractor.extract(this)
