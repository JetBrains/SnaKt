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

private val localityAnnotationId = annotationId("Borrowed")

private fun FirDeclaration.hasLocalityAnnotation(): Boolean {
    return annotations.any { it.annotationTypeRef.coneType.classId == localityAnnotationId }
}

private inline fun FirDeclaration.extractLocality(
    findOwner: FirDeclaration.() -> FirBasedSymbol<*>?
): Locality {
    if (!hasLocalityAnnotation()) return Locality.Global

    return Locality.Local(findOwner())
}

context(context: CheckerContext)
fun FirReceiverParameter.extractRequiredLocality(): Locality =
    extractLocality {
        context.findClosest()
    }

fun FirReceiverParameter.extractActualLocality(): Locality =
    extractLocality {
        containingDeclarationSymbol
    }

context(context: CheckerContext)
fun FirValueParameter.extractRequiredLocality(): Locality =
    extractLocality {
        context.findClosest()
    }

fun FirValueParameter.extractActualLocality(): Locality =
    extractLocality {
        containingDeclarationSymbol
    }

private fun FirControlFlowGraphOwner.declaresProperty(property: FirProperty): Boolean =
    controlFlowGraphReference?.controlFlowGraph?.nodes?.any { node ->
        node is VariableDeclarationNode && node.fir == property
    } ?: false

@OptIn(SymbolInternals::class)
context(context: CheckerContext)
private fun FirProperty.findOwner(): FirBasedSymbol<*>? =
    context.findClosest { declarationSymbol ->
        val declaration = declarationSymbol.fir as? FirControlFlowGraphOwner
            ?: return@findClosest false
        declaration.declaresProperty(this)
    }

context(context: CheckerContext)
fun FirProperty.extractLocality(): Locality =
    extractLocality {
        findOwner()
    }
