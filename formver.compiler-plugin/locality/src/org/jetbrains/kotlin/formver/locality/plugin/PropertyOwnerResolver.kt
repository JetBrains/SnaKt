/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

/**
 * Resolves the owner of a local property.
 */
class PropertyOwnerResolver(
    session: FirSession
) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> PropertyOwnerResolver(session) }
        }
    }

    private val cachesFactory = session.firCachesFactory

    private fun FirControlFlowGraphOwner.declaresProperty(property: FirProperty): Boolean =
        controlFlowGraphReference?.controlFlowGraph?.nodes?.any { node ->
            node is VariableDeclarationNode && node.fir == property
        } ?: false

    @OptIn(SymbolInternals::class)
    private fun CheckerContext.findOwnerOf(property: FirProperty): FirBasedSymbol<*>? =
        findClosest { declarationSymbol ->
            val declaration = declarationSymbol.fir as? FirControlFlowGraphOwner
                ?: return@findClosest false
            declaration.declaresProperty(property)
        }

    @OptIn(SymbolInternals::class)
    private val cache = cachesFactory.createCache { key: FirProperty, context: CheckerContext ->
        with(context) {
            findOwnerOf(key)
        }
    }

    context(context: CheckerContext)
    fun resolveOwnerOf(property: FirProperty): FirBasedSymbol<*>? =
        cache.getValue(property, context)
}

val FirSession.propertyOwnerResolver: PropertyOwnerResolver
        by FirSession.sessionComponentAccessor<PropertyOwnerResolver>()

/**
 * Resolves the owning declaration of `this` local property.
 *
 * Returns `null` if the owner cannot be resolved.
 */
context(context: CheckerContext)
fun FirProperty.resolveOwner(): FirBasedSymbol<*>? =
    context.session.propertyOwnerResolver.resolveOwnerOf(this)
