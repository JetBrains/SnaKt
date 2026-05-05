/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

interface JoinSemilatticeElement<Self : JoinSemilatticeElement<Self>> {
    /**
     * Computes the least upper bound of `this` and [other].
     */
    fun union(other: Self): Self
}
