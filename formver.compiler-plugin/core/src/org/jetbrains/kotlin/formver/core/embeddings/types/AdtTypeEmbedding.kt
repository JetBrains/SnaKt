/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.fir.types.ConeKotlinType
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
    val originalType: ConeKotlinType,
)

sealed interface AdtTypeEmbedding : PretypeEmbedding

data object InvalidAdtTypeEmbedding : AdtTypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.nothingType()
    override val name = PretypeName("Invalid")
}

val RuntimeTypeHolder.nonStandaloneAdtTypeEmbedding: AdtTypeEmbeddingImpl?
    get() {
        val pretype = when (this) {
            is TypeEmbedding -> pretype
            is AdtTypeEmbeddingImpl -> this
            else -> return null
        }
        return (pretype as? AdtTypeEmbeddingImpl)?.takeIf { it.standalone !== it }
    }

class AdtTypeEmbeddingImpl(override val name: ScopedName) : AdtTypeEmbedding {
    private val ownAdtName: AdtName = AdtName(name)
    private val ownViperType: Type.Adt = Type.Adt(ownAdtName)
    private val ownInjection: Injection = Injection(name, ownViperType, RuntimeTypeDomain.classTypeFunc(name))

    // For sum-type children, standalone points to the parent sealed interface embedding,
    // so all Viper-level operations use a single shared ADT declaration.
    var standalone: AdtTypeEmbeddingImpl = this
        internal set

    val adtName: AdtName get() = standalone.ownAdtName
    val viperType: Type.Adt get() = standalone.ownViperType
    val injection: Injection get() = standalone.ownInjection

    override val runtimeType get() = standalone.ownInjection.typeFunction()

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
