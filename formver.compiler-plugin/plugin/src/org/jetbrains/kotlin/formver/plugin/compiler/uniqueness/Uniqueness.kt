/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.uniqueness

sealed interface Uniqueness {
    data object Moved : Uniqueness

    data object Shared : Uniqueness

    data object Unique : Uniqueness

    fun join(other: Uniqueness): Uniqueness =
        when (this) {
            Moved -> Moved
            Shared -> when (other) {
                Moved -> Moved
                else -> Shared
            }
            Unique -> when (other) {
                Moved -> Moved
                Shared -> Shared
                Unique -> Unique
            }
        }

    fun accepts(other: Uniqueness): Boolean =
        when (this) {
            Shared -> other != Moved
            Unique -> other == Unique
            else -> false
        }

    fun render(): String =
        when (this) {
            Moved -> "moved"
            Shared -> "shared"
            Unique -> "unique"
        }
}
