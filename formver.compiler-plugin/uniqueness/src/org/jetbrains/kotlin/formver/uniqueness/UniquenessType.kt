/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

enum class UniqueLevel {
    Unique,
    Shared,
}

enum class BorrowLevel {
    Free,
    Borrowed
}

sealed interface UniquenessType {

    /**
     * Corresponds to the TOP type
     */
    object Moved : UniquenessType

    /**
     * Intermediate components of the lattice
     */
    data class Active(val uniqueLevel: UniqueLevel, val borrowLevel: BorrowLevel) : UniquenessType

    /**
     * Join operation for the uniqueness type lattice.
     *
     * @param other The other type to join with.
     * @return The result of the join operation.
     */
    fun join(other: UniquenessType): UniquenessType {
        if (this == other) {
            return this
        }

        if (this is Moved || other is Moved) {
            return Moved
        }

        this as Active
        other as Active

        return Active(
            maxOf(uniqueLevel, other.uniqueLevel),
            maxOf(borrowLevel, other.borrowLevel)
        )
    }

}