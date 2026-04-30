package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.formver.core.domains.FunctionBuilder
import org.jetbrains.kotlin.formver.core.domains.Injection
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain.Companion.isOf
import org.jetbrains.kotlin.formver.core.names.AdtConstructorName
import org.jetbrains.kotlin.formver.core.names.AdtEqualityFunctionName
import org.jetbrains.kotlin.formver.core.names.AdtName
import org.jetbrains.kotlin.formver.core.names.ScopedName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.*
import org.jetbrains.kotlin.formver.viper.ast.Function

data class AdtTypeEmbedding(override val name: ScopedName) : PretypeEmbedding {
    val adtName: AdtName = AdtName(name)
    val viperType: Type.Adt = Type.Adt(adtName)
    val injection: Injection = Injection(name, viperType, RuntimeTypeDomain.classTypeFunc(name))

    override val runtimeType = RuntimeTypeDomain.classTypeFunc(name)()

    val viperConstructorDecl: AdtConstructorDecl
        get() = AdtConstructorDecl(AdtConstructorName(adtName), adtName, emptyList())

    val equalityFunctionName: SymbolicName = AdtEqualityFunctionName(adtName)

    fun toViper() = AdtDecl(name = adtName, constructors = listOf(viperConstructorDecl))

    fun equalityFunction(): Function = FunctionBuilder.build(equalityFunctionName) {
        val lhs = argument(Type.Ref)
        val rhs = argument(Type.Ref)
        returns(Type.Bool)
        precondition { lhs isOf runtimeType }
        body { rhs isOf runtimeType }
    }
}
