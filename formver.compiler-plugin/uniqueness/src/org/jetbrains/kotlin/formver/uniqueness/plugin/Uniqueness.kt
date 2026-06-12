/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.formver.type.plugin.TypeIntersector
import org.jetbrains.kotlin.formver.type.plugin.TypeJudgment
import org.jetbrains.kotlin.formver.type.plugin.TypeUnifier

enum class Uniqueness {
    Unique,
    Shared,
    Accessed,
    Moved
}

val ConeKotlinType.defaultUniqueness: Uniqueness
    get() = attributes.uniquenessAttribute?.uniqueness ?: Uniqueness.Shared

fun Uniqueness.accepts(other: Uniqueness): Boolean =
    this >= other

object UniquenessJudgment : TypeJudgment<Uniqueness> {
    override fun satisfies(requiredType: Uniqueness, actualType: Uniqueness): Boolean =
        requiredType.accepts(actualType)
}

fun Uniqueness.join(other: Uniqueness): Uniqueness =
    maxOf(this, other)

object UniquenessUnifier : TypeUnifier<Uniqueness> {
    override fun join(left: Uniqueness, right: Uniqueness): Uniqueness {
        return left.join(right)
    }
}

fun Uniqueness.meet(other: Uniqueness): Uniqueness =
    minOf(this, other)

object UniquenessIntersector : TypeIntersector<Uniqueness> {
    override fun meet(left: Uniqueness, right: Uniqueness): Uniqueness {
        return left.meet(right)
    }
}

val UniquenessRenderer = Renderer<Uniqueness> { uniqueness ->
    when (uniqueness) {
        Uniqueness.Unique -> "unique"
        Uniqueness.Shared -> "shared"
        Uniqueness.Accessed -> "accessed"
        Uniqueness.Moved -> "moved"
    }
}
