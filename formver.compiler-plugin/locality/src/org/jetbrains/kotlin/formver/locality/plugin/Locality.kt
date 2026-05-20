/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.types.ConeKotlinType

enum class Locality {
    Local, Global
}

fun Locality.join(other: Locality): Locality =
    minOf(this, other)

fun Locality.accepts(other: Locality): Boolean =
    this <= other

val ConeKotlinType.locality: Locality
    get() = if (attributes.locality != null) {
        Locality.Local
    } else {
        Locality.Global
    }

val LocalityRenderer = Renderer<Locality> { locality ->
    when (locality) {
        Locality.Global -> "global"
        Locality.Local -> "local"
    }
}
