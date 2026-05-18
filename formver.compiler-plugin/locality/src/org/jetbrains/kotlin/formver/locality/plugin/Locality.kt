/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.formver.type.plugin.TypeIntersector
import org.jetbrains.kotlin.formver.type.plugin.TypeJudgment
import org.jetbrains.kotlin.formver.type.plugin.TypeUnifier
import org.jetbrains.kotlin.fir.types.ConeKotlinType

enum class Locality {
    Local, Global
}

fun Locality.accepts(other: Locality): Boolean =
    this <= other

object LocalityJudgment : TypeJudgment<Locality> {
    override fun satisfies(requiredType: Locality, actualType: Locality): Boolean =
        requiredType.accepts(actualType)
}

fun Locality.join(other: Locality): Locality =
    minOf(this, other)

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

object LocalityUnifier : TypeUnifier<Locality> {
    override fun join(left: Locality, right: Locality): Locality {
        return left.join(right)
    }
}

fun Locality.meet(other: Locality): Locality =
    maxOf(this, other)

object LocalityIntersector : TypeIntersector<Locality> {
    override fun meet(left: Locality, right: Locality): Locality {
        return left.meet(right)
    }
}
