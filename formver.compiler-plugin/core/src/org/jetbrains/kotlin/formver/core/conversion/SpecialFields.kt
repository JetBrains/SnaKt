/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion


import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.FieldAccess
import org.jetbrains.kotlin.formver.core.embeddings.expression.IntLit
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.*
import org.jetbrains.kotlin.formver.core.kotlinClassId
import org.jetbrains.kotlin.formver.core.names.SpecialFieldName
import org.jetbrains.kotlin.formver.core.names.embedName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Type

class SpecialField(
    val baseName: String,
    override val type: TypeEmbedding,
    override val viperType: Type,
    override val includeInShortDump: Boolean = false,
    override val accessPolicy: AccessPolicy = AccessPolicy.ALWAYS_WRITEABLE,
    override val containingClass: ClassTypeEmbedding? = null,
    private val extraInvariantsBuilder: (FieldEmbedding) -> List<TypeInvariantEmbedding> = { listOf() },
) : FieldEmbedding {
    override val name: SymbolicName = SpecialFieldName(baseName)
    override fun extraAccessInvariantsForParameter(): List<TypeInvariantEmbedding> = extraInvariantsBuilder(this)
}

val CollectionSizeFieldEmbedding: FieldEmbedding = SpecialField(
    baseName = "size",
    type = buildType { int() },
    viperType = Type.Ref,
    includeInShortDump = true,
) { field ->
    listOf(object : TypeInvariantEmbedding {
        override fun fillHole(exp: ExpEmbedding): ExpEmbedding =
            OperatorExpEmbeddings.GeIntInt(FieldAccess(exp, field), IntLit(0))
    })
}


val IntArrayData: SpecialField = SpecialField(
    baseName = "data",
    type = buildType {
        intArray()
    },
    viperType = Type.Seq(Type.Int),
    includeInShortDump = true,
    accessPolicy = AccessPolicy.BY_RECEIVER_UNIQUENESS,
    containingClass = buildClassPretype {
        withName(kotlinClassId("IntArray").embedName())
    }
)
