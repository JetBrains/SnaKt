/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.extensions.FirTypeAttributeExtension
import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.FirTypeRef
import kotlin.reflect.KClass

enum class UniqueLevel {
    Unique,
    Shared,
}

enum class BorrowLevel {
    Global,
    Local
}

sealed interface UniquenessType {
    /**
     * Corresponds to the TOP type
     */
    object Moved : UniquenessType {
        override fun toString() = "moved"
    }

    /**
     * Intermediate components of the lattice
     */
    data class Active(val uniqueLevel: UniqueLevel, val borrowLevel: BorrowLevel) : UniquenessType {
        override fun toString() = "${uniqueLevel.name.lowercase()} ${borrowLevel.name.lowercase()}"
    }

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

        when (this) {
            is Moved -> return this
            is Active -> {
                when (other) {
                    is Moved -> return other
                    is Active -> {
                        return Active(
                            maxOf(uniqueLevel, other.uniqueLevel),
                            maxOf(borrowLevel, other.borrowLevel)
                        )
                    }
                }
            }
        }
    }
}
