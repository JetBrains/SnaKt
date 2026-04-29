package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.formver.core.domains.Injection
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.names.AdtConstructorName
import org.jetbrains.kotlin.formver.core.names.AdtName
import org.jetbrains.kotlin.formver.core.names.ScopedName
import org.jetbrains.kotlin.formver.viper.ast.*

data class AdtTypeEmbedding(override val name: ScopedName) : PretypeEmbedding {
    private var _isInitialized: Boolean = false

    val isInitialized: Boolean
        get() = _isInitialized

    fun markInitialized() {
        require(!_isInitialized) { "ADT $name is already initialised." }
        _isInitialized = true
    }

    val adtName: AdtName = AdtName(name)
    val viperType: Type.Adt = Type.Adt(adtName)
    val injection: Injection = Injection(AdtName(name, "ToRef"), AdtName(name, "FromRef"), viperType, RuntimeTypeDomain.classTypeFunc(name))

    override val runtimeType = RuntimeTypeDomain.classTypeFunc(name)()

    override fun accessInvariants(): List<TypeInvariantEmbedding> = emptyList()
    override fun sharedPredicateAccessInvariant() = null
    override fun uniquePredicateAccessInvariant() = null

    val viperConstructorDecl: AdtConstructorDecl
        get() = AdtConstructorDecl(AdtConstructorName(adtName, name), adtName, emptyList())

    fun toViper() = AdtDecl(name = adtName, constructors = listOf(viperConstructorDecl))
}