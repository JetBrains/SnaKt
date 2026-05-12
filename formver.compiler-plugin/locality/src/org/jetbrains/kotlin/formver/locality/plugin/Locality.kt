/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.types.ConeKotlinType

typealias Locality = LocalityAttribute?

val ConeKotlinType.locality: Locality
    get() = attributes.locality

fun Locality.meet(other: Locality): Locality =
    this?.intersect(other)

fun Locality.join(other: Locality): Locality =
    this?.union(other) ?: other

fun Locality.accepts(other: Locality?): Boolean =
    this != null || other == null

val LocalityRenderer = Renderer<Locality> { locality ->
    when (locality) {
        null -> "global"
        LocalityAttribute -> "local"
    }
}
