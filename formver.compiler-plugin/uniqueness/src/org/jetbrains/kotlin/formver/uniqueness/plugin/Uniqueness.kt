/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.formver.type.plugin.TypeFactIntersector
import org.jetbrains.kotlin.formver.type.plugin.TypeFactJudgment
import org.jetbrains.kotlin.formver.type.plugin.TypeFactUnifier

enum class Uniqueness {
    Unique,
    Shared,
    Moved
}

val ConeKotlinType.defaultUniqueness: Uniqueness
    get() = attributes.uniquenessAttribute?.uniqueness ?: Uniqueness.Shared

fun Uniqueness.accepts(other: Uniqueness): Boolean =
    this >= other

object UniquenessJudgment : TypeFactJudgment<Uniqueness> {
    override fun satisfies(requiredTypeFact: Uniqueness, actualTypeFact: Uniqueness): Boolean =
        requiredTypeFact.accepts(actualTypeFact)
}

fun Uniqueness.join(other: Uniqueness): Uniqueness =
    maxOf(this, other)

object UniquenessUnifier : TypeFactUnifier<Uniqueness> {
    override fun join(left: Uniqueness, right: Uniqueness): Uniqueness {
        return left.join(right)
    }
}

fun Uniqueness.meet(other: Uniqueness): Uniqueness =
    minOf(this, other)

object UniquenessIntersector : TypeFactIntersector<Uniqueness> {
    override fun meet(left: Uniqueness, right: Uniqueness): Uniqueness {
        return left.meet(right)
    }
}

val UniquenessRenderer = Renderer<Uniqueness> { uniqueness ->
    when (uniqueness) {
        Uniqueness.Unique -> "unique"
        Uniqueness.Shared -> "shared"
        Uniqueness.Moved -> "moved"
    }
}
