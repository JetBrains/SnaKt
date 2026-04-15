package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.names.ScopedKotlinName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.*

/**
 * Embedding-level representation of an ADT constructor.
 *
 * Each Kotlin class annotated declared as an ADT maps to exactly one constructor.
 */
class AdtConstructorEmbedding(
    val className: SymbolicName,
)

data class AdtTypeEmbedding(override val name: ScopedKotlinName) : PretypeEmbedding {
    private var _constructors: List<AdtConstructorEmbedding>? = null
    val constructors: List<AdtConstructorEmbedding>
        get() = _constructors ?: error("ADT constructors of $name have not been initialised yet.")

    val isInitialized: Boolean
        get() = _constructors != null

    fun initConstructors(newConstructors: List<AdtConstructorEmbedding>) {
        require(_constructors == null) { "ADT constructors of $name are already initialised." }
        _constructors = newConstructors
    }

    val adtName: AdtName = AdtName(name)
    val viperType: Type.Adt = Type.Adt(adtName)

    val toRefFunc: DomainFunc = RuntimeTypeDomain.adtToRefFunc(name, viperType)
    val fromRefFunc: DomainFunc = RuntimeTypeDomain.adtFromRefFunc(name, viperType)

    override val runtimeType = RuntimeTypeDomain.classTypeFunc(name)()

    override fun accessInvariants(): List<TypeInvariantEmbedding> = emptyList()
    override fun sharedPredicateAccessInvariant() = null
    override fun uniquePredicateAccessInvariant() = null
}