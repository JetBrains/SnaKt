package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.formver.core.embeddings.SourceRole
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.PrimitiveFieldAccess
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.withType
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding

/**
 * Path abstraction to handle paths. The core functionality is to associate the `ExpEmbedding` paths with the `firPaths`.
 * This is necessary, because the uniqueness checker works on the fir level, whereas the permission management works
 * on the `ExpEmbedding` level.
 */
sealed interface Path {
    fun addField(field: FieldEmbedding): Path {
        return PathElement(this, field)
    }

    val length: Int

    val firPath: List<FirBasedSymbol<*>>

    fun pathWithoutLast(): Path?

    val type: TypeEmbedding

    val expEmbedding: ExpEmbedding

    /**
     * Returns a list of all prefixes from the path, ordered ascending by the length.
     */
    fun traverse(): List<Path>

}


data class PathRoot(val base: VariableEmbedding) : Path {
    override val length: Int
        get() = 1

    override val firPath = listOf((base.sourceRole!! as SourceRole.FirSymbolHolder).firSymbol)
    override val type = base.type
    override val expEmbedding = base

    override fun pathWithoutLast(): Path? = null
    override fun traverse() = listOf(this)
}

data class PathElement(val base: Path, val field: FieldEmbedding) : Path {
    override val length: Int
        get() = base.length + 1

    override val firPath = base.firPath + field.symbol!!
    override val type = field.type
    override val expEmbedding = PrimitiveFieldAccess(base.expEmbedding, field)

    override fun pathWithoutLast(): Path = base

    override fun traverse() = base.traverse() + this

}

data class PathCast(val base: Path, val newType: TypeEmbedding) : Path {

    override val length: Int = base.length
    override val firPath: List<FirBasedSymbol<*>> = base.firPath
    override fun pathWithoutLast(): Path? = base.pathWithoutLast()
    override val type: TypeEmbedding = newType
    override val expEmbedding: ExpEmbedding = base.expEmbedding.withType(newType)
    override fun traverse(): List<Path> = base.traverse().dropLast(1) + this
}