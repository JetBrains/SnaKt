/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.formver.core.domains.Injection
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.names.AdtConstructorName
import org.jetbrains.kotlin.formver.core.names.AdtFieldName
import org.jetbrains.kotlin.formver.core.names.AdtName
import org.jetbrains.kotlin.formver.core.names.PretypeName
import org.jetbrains.kotlin.formver.core.names.ScopedName
import org.jetbrains.kotlin.formver.viper.ast.*

data class AdtFieldEmbedding(
    val name: AdtFieldName,
    val type: TypeEmbedding,
)

sealed interface AdtTypeEmbedding : PretypeEmbedding

data object InvalidAdtTypeEmbedding : AdtTypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.nothingType()
    override val name = PretypeName("Invalid")
}

data class AdtTypeEmbeddingImpl(override val name: ScopedName) : AdtTypeEmbedding {
    val adtName: AdtName = AdtName(name)
    val viperType: Type.Adt = Type.Adt(adtName)
    val injection: Injection = Injection(name, viperType, RuntimeTypeDomain.classTypeFunc(name))

    override val runtimeType = RuntimeTypeDomain.classTypeFunc(name)()

    fun getViperConstructorDecl(fields: List<AdtFieldEmbedding>): AdtConstructorDecl =
        AdtConstructorDecl(
            AdtConstructorName(adtName, name),
            adtName,
            fields.map { Declaration.LocalVarDecl(it.name, Type.Ref) },
        )

    fun toAdtDecl(fields: List<AdtFieldEmbedding>): AdtDecl =
        AdtDecl(
            name = adtName,
            constructors = listOf(getViperConstructorDecl(fields)),
        )
}
