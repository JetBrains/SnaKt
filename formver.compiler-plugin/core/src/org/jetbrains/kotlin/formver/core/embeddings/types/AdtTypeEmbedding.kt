package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.names.ScopedKotlinName
import org.jetbrains.kotlin.formver.viper.ast.AlgebraicDataType

data class AdtTypeEmbedding(override val name: ScopedKotlinName) : PretypeEmbedding {
    var adtDeclaration: AlgebraicDataType? = null

    override val runtimeType = RuntimeTypeDomain.classTypeFunc(name)()

    override fun accessInvariants(): List<TypeInvariantEmbedding> = emptyList()
    override fun sharedPredicateAccessInvariant() = null
    override fun uniquePredicateAccessInvariant() = null
}