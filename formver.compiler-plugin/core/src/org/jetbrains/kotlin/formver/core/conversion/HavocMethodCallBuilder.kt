package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.PretypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbeddingFlags
import org.jetbrains.kotlin.formver.core.names.SpecialName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt


abstract class HavocMethodCallBuilder {
    val target: Exp.LocalVar
        get() = _target ?: throw IllegalStateException("Target not set")

    val flags: TypeEmbeddingFlags
        get() = _flags ?: throw IllegalStateException("Flags not set")

    private var _target: Exp.LocalVar? = null
    private var _flags: TypeEmbeddingFlags? = null

    fun withTarget(target: Exp.LocalVar): HavocMethodCallBuilder {
        _target = target
        return this
    }

    fun withFlags(flags: TypeEmbeddingFlags): HavocMethodCallBuilder {
        _flags = flags
        return this
    }

    abstract fun build(): Stmt.MethodCall
}


class PrimitiveHavocMethodCallBuilder(private val preType: PretypeEmbedding) : HavocMethodCallBuilder() {
    override fun build(): Stmt.MethodCall =
        Stmt.MethodCall(SpecialName("havoc"), listOf(flags.adjustRuntimeType(preType.runtimeType)), listOf(target))
}

class ClassHavocMethodCallBuilder(private val preType: ClassTypeEmbedding) : HavocMethodCallBuilder() {
    override fun build(): Stmt.MethodCall {
        val method = if (flags.nullable) {
            preType.details.havocMethodNullable
        } else {
            preType.details.havocMethod
        }
        return method.toMethodCall(emptyList(), listOf(target))
    }
}
