/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType

private inline fun FirTypeRef.resolveLocality(findOwner: () -> FirBasedSymbol<*>?): Locality {
    if (coneType.attributes.locality == null) return Global

    return Local(findOwner() ?: UnknownSymbol)
}

private fun CheckerContext.findClosestFunction(): FirBasedSymbol<*>? =
    findClosest<FirFunctionSymbol<*>>()

context(context: CheckerContext)
fun FirReceiverParameter.extractRequiredLocality(): Locality =
    typeRef.resolveLocality { context.findClosestFunction() }

fun FirReceiverParameter.extractActualLocality(): Locality =
    typeRef.resolveLocality { containingDeclarationSymbol }

context(context: CheckerContext)
fun FirValueParameter.extractRequiredLocality(): Locality =
    returnTypeRef.resolveLocality { context.findClosestFunction() }

fun FirValueParameter.extractActualLocality(): Locality =
    returnTypeRef.resolveLocality { containingDeclarationSymbol }

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
    returnTypeRef.resolveLocality { findOwner() }
