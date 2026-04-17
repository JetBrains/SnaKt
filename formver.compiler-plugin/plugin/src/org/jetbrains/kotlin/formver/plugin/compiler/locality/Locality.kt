/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.formver.plugin.compiler.locality.Locality.Global
import org.jetbrains.kotlin.formver.plugin.compiler.locality.Locality.Local

sealed interface Locality {
    data object Global : Locality

    data class Local(
        val owner: FirBasedSymbol<*>? = null
    ) : Locality

    fun join(other: Locality): Locality =
        when (other) {
            Global -> this
            is Local -> {
                when (this) {
                    Global -> other
                    is Local -> {
                        if (owner == other.owner) {
                            this
                        } else {
                            Local() // Unknown owner
                        }
                    }
                }
            }
        }

    fun accepts(other: Locality): Boolean =
        when (this) {
            Global -> other == Global
            is Local -> when (other) {
                Global -> true
                is Local -> owner == other.owner
            }
        }
}

val LocalityRenderer = Renderer<Locality> { locality ->
    when (locality) {
        Global -> "'global'"
        is Local -> "'local(${(locality.owner as? FirCallableSymbol<*>)?.name ?: "unknown"})'"
    }
}
