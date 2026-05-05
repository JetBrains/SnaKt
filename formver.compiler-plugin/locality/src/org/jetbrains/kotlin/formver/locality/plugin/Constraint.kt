/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext

interface Constraint<T: AbstractValue<*>> {
    /**
     * Returns `true` if `this` requirement accepts [value], `false` otherwise.
     */
    context(context: CheckerContext)
    fun accepts(value: T): Boolean
}
