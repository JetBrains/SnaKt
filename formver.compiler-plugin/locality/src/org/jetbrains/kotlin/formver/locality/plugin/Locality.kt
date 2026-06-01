/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.formver.type.plugin.TypeIntersector
import org.jetbrains.kotlin.formver.type.plugin.TypeJudgment
import org.jetbrains.kotlin.formver.type.plugin.TypeUnifier

typealias Locality = LocalityAttribute?

val ConeKotlinType.locality: Locality
    get() = attributes.locality

fun Locality.accepts(other: Locality): Boolean =
    this != null || other == null

object LocalityJudgment : TypeJudgment<Locality> {
    override fun satisfies(requiredType: Locality, actualType: Locality): Boolean =
        requiredType.accepts(actualType)
}

fun Locality.join(other: Locality): Locality =
    this?.union(other) ?: other

object LocalityUnifier : TypeUnifier<Locality> {
    override fun join(left: Locality, right: Locality): Locality {
        return left.join(right)
    }
}

fun Locality.meet(other: Locality): Locality =
    this?.intersect(other)

object LocalityIntersector : TypeIntersector<Locality> {
    override fun meet(left: Locality, right: Locality): Locality {
        return left.meet(right)
    }
}

val LocalityRenderer = Renderer<Locality> { locality ->
    when (locality) {
        null -> "global"
        LocalityAttribute -> "local"
    }
}
