/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

sealed interface Locality

data object Global : Locality

data class Local(
    val owner: FirBasedSymbol<*>
) : Locality

object UnknownSymbol : FirBasedSymbol<FirDeclaration>(), SyntheticSymbol

fun Locality.accepts(other: Locality): Boolean {
    return when (this) {
        Global -> other == Global
        is Local ->
            when (other) {
                Global -> true
                is Local -> owner == UnknownSymbol || owner == other.owner
            }
    }
}

fun Locality.union(other: Locality): Locality {
    return when {
        this == other -> this
        this is Global -> other
        other is Global -> this
        else -> Local(UnknownSymbol)
    }
}

val LocalityRenderer = Renderer<Locality> { locality ->
    when (locality) {
        Global -> "'global'"
        is Local -> "'local(${(locality.owner as? FirCallableSymbol<*>)?.name ?: "unknown"})'"
    }
}
