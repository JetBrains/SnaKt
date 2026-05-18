/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.plugin


/**
 * Finds the greatest lower-bound of two types.
 */
fun interface TypeIntersector<Type> {
    fun meet(left: Type, right: Type): Type
}

/**
 * Checks whether a type satisfies a given type requirement.
 */
fun interface TypeJudgment<Type> {
    fun satisfies(requiredType: Type, actualType: Type): Boolean
}

/**
 * Finds the least upper-bound of two types.
 */
fun interface TypeUnifier<Type> {
    fun join(left: Type, right: Type): Type
}
