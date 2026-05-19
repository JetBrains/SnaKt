/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.properties

import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.FieldAccessTypeInvariantEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeInvariantEmbedding
import org.jetbrains.kotlin.formver.core.names.ScopedName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Field
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Type

/**
 * Embedding of a backing field of a property.
 */
interface FieldEmbedding {
    val name: SymbolicName
    val type: TypeEmbedding
    val viperType: Type
    val accessPolicy: AccessPolicy
    val isUnique: Boolean
        get() = false

    // If true, it is necessary to unfold the predicate of the receiver before accessing the field
    val unfoldToAccess: Boolean
        get() = false
    val containingClass: ClassTypeEmbedding?
        get() = null
    val includeInShortDump: Boolean
    val symbol: FirPropertySymbol?
        get() = null

    fun toViper(): Field = Field(name, viperType, includeInShortDump)

    fun extraAccessInvariantsForParameter(): List<TypeInvariantEmbedding> = listOf()

    fun accessInvariantsForParameter(): List<TypeInvariantEmbedding> =
        when (accessPolicy) {
            AccessPolicy.ALWAYS_WRITEABLE -> listOf(FieldAccessTypeInvariantEmbedding(this, PermExp.FullPerm()))
            AccessPolicy.BY_RECEIVER_UNIQUENESS, AccessPolicy.MANUAL -> listOf()
        } + extraAccessInvariantsForParameter()

}

data class UserFieldEmbedding(
    override val name: ScopedName,
    override val type: TypeEmbedding,
    override val symbol: FirPropertySymbol,
    override val isUnique: Boolean,
    override val containingClass: ClassTypeEmbedding,
    val isManual: Boolean
) : FieldEmbedding {
    override val viperType = Type.Ref
    override val accessPolicy: AccessPolicy =
        when {
            isManual -> AccessPolicy.MANUAL
            symbol.isVar -> AccessPolicy.BY_RECEIVER_UNIQUENESS
            else -> throw SnaktInternalException(
                symbol.initializerSource,
                "Failed to determine AccessPolicy. Field is neither val nor var."
            )
        }
    override val unfoldToAccess: Boolean
        get() = accessPolicy == AccessPolicy.BY_RECEIVER_UNIQUENESS
    override val includeInShortDump: Boolean = true
}
