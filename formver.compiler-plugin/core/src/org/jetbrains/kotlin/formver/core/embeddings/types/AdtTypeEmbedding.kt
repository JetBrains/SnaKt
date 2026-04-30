package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.formver.core.domains.Injection
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.names.AdtConstructorName
import org.jetbrains.kotlin.formver.core.names.AdtName
import org.jetbrains.kotlin.formver.core.names.ScopedName
import org.jetbrains.kotlin.formver.viper.ast.*

data class AdtTypeEmbedding(override val name: ScopedName) : PretypeEmbedding {
    val adtName: AdtName = AdtName(name)
    val viperType: Type.Adt = Type.Adt(adtName)
    val injection: Injection = Injection(name, viperType, RuntimeTypeDomain.classTypeFunc(name))

    override val runtimeType = RuntimeTypeDomain.classTypeFunc(name)()

    val viperConstructorDecl: AdtConstructorDecl
        get() = AdtConstructorDecl(AdtConstructorName(adtName, name), adtName, emptyList())

    fun toViper() = AdtDecl(name = adtName, constructors = listOf(viperConstructorDecl))
}
