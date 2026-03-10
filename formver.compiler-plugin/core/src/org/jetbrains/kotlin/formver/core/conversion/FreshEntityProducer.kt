/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

/**
 * A stateful factory that produces fresh instances of type [R] by supplying a monotonically
 * increasing integer index together with a caller-provided seed value of type [S].
 *
 * The index guarantees uniqueness across all entities produced by the same instance, which is
 * used to generate distinct Viper names for anonymous variables, return targets, scope indices, etc.
 *
 * @param build Function that constructs an [R] from an index and a seed [S].
 */
class FreshEntityProducer<R, S>(private val build: (Int, S) -> R) {
    // Next index to hand out; incremented on every call to getFresh.
    private var next = 0

    /** Returns a fresh [R] built from the current index (then advances the counter) and [s]. */
    fun getFresh(s: S): R = build(next++, s)
}

/**
 * Specialisation of [FreshEntityProducer] where no seed value is needed.
 * The index alone is sufficient to construct the entity.
 */
typealias SimpleFreshEntityProducer<R> = FreshEntityProducer<R, Unit>

/**
 * Convenience constructor for a [SimpleFreshEntityProducer]:
 * wraps a single-argument [build] function so the unused [Unit] seed is hidden from callers.
 */
fun <R> simpleFreshEntityProducer(build: (Int) -> R): SimpleFreshEntityProducer<R> =
    FreshEntityProducer { n, _ -> build(n) }

/** Calls [getFresh] on a [SimpleFreshEntityProducer] without requiring a seed argument. */
fun <R> SimpleFreshEntityProducer<R>.getFresh() = getFresh(Unit)

/** Produces fresh [ScopeIndex.Indexed] values, used to distinguish nested scopes. */
fun scopeIndexProducer(): SimpleFreshEntityProducer<ScopeIndex.Indexed> = simpleFreshEntityProducer(ScopeIndex::Indexed)

/** Produces fresh raw integer indices, used e.g. for while-loop numbering. */
fun indexProducer() = simpleFreshEntityProducer { it }