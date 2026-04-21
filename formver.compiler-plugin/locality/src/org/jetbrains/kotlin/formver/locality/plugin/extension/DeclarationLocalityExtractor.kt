/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin.extension

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

private inline fun FirTypeRef.extractLocality(findOwner: () -> FirBasedSymbol<*>?): LocalityAttribute? =
    coneType.attributes.locality?.copy(owner = findOwner())

private fun CheckerContext.findClosestFunction(): FirBasedSymbol<*>? =
    findClosest<FirFunctionSymbol<*>>()

context(context: CheckerContext)
fun FirReceiverParameter.extractRequiredLocality(): LocalityAttribute? =
    typeRef.extractLocality { context.findClosestFunction() }

fun FirReceiverParameter.extractActualLocality(): LocalityAttribute? =
    typeRef.extractLocality { containingDeclarationSymbol }

context(context: CheckerContext)
fun FirValueParameter.extractRequiredLocality(): LocalityAttribute? =
    returnTypeRef.extractLocality { context.findClosestFunction() }

fun FirValueParameter.extractActualLocality(): LocalityAttribute? =
    returnTypeRef.extractLocality { containingDeclarationSymbol }

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
fun FirProperty.extractLocality(): LocalityAttribute? =
    returnTypeRef.extractLocality { findOwner() }
