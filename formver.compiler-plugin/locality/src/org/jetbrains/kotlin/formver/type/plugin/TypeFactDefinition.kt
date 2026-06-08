/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.plugin


/**
 * Finds the greatest lower-bound of two type facts.
 */
fun interface TypeFactIntersector<TypeFact> {
    fun meet(left: TypeFact, right: TypeFact): TypeFact
}

/**
 * Checks whether a type fact satisfies a given type-fact requirement.
 */
fun interface TypeFactJudgment<TypeFact> {
    fun satisfies(requiredTypeFact: TypeFact, actualTypeFact: TypeFact): Boolean
}

/**
 * Finds the least upper-bound of two type facts.
 */
fun interface TypeFactUnifier<TypeFact> {
    fun join(left: TypeFact, right: TypeFact): TypeFact
}
