/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

interface AbstractValue<Self : AbstractValue<Self>> {
    /**
     * Merges `this` abstract value with [other]. If both `this` and [other] are local to different declarations the
     * result will be `Local(null)` (local to unknown).
     */
    fun union(other: Self): Self
}
