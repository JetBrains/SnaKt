/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.uniqueness

/**
 * Common building blocks to extract expression uniqueness.
 *
 * [D] is the visitor data type threaded through the expression traversal.
 */
abstract class ExpressionUniquenessExtractor<D> : PathValueExtractor<Uniqueness, D>() {
    override val empty = Uniqueness.Unique

    override fun Uniqueness.join(other: Uniqueness): Uniqueness {
        return join(other)
    }
}
