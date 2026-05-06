/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.formver.locality.plugin.Locality.Global
import org.jetbrains.kotlin.formver.locality.plugin.Locality.Local

/**
 * Symbolic locality value.
 */
sealed interface Locality {

    /**
     * Symbolic locality value for a global reference.
     */
    data object Global : Locality

    /**
     * Symbolic locality value for a reference that is local to the declaration referred to by `owner`.
     */
    data class Local(
        val owner: FirBasedSymbol<*>?
    ) : Locality
}

/**
 * Returns `true` if `this` locality accepts [other], `false` otherwise.
 */
fun Locality.accepts(other: Locality): Boolean {
    return when (this) {
        Global -> other == Global
        is Local ->
            when (other) {
                Global -> true
                is Local -> owner == null || owner == other.owner
            }
    }
}

/**
 * Merges `this` locality with [other]. If both `this` and [other] are local to different declarations the result will
 * be `Local(null)` (local to unknown).
 */
fun Locality.union(other: Locality): Locality {
    return when {
        this == other -> this
        this is Global -> other
        other is Global -> this
        else -> Local(null)
    }
}

/**
 * Renders a locality value for diagnostics.
 */
val LocalityRenderer = Renderer<Locality> { locality ->
    when (locality) {
        Global -> "'global'"
        is Local -> "'local(${(locality.owner as? FirCallableSymbol<*>)?.name ?: "unknown"})'"
    }
}
