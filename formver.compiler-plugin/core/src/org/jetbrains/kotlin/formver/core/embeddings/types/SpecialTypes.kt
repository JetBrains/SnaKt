package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.formver.core.kotlinClassId

interface SpecialType {
    val typeEmbedding: PretypeEmbedding

    fun match(symbol: ConeKotlinType, session: FirSession): Boolean
}


object IntArray : SpecialType {
    override val typeEmbedding: ClassTypeEmbedding = IntArrayTypeEmbedding

    override fun match(symbol: ConeKotlinType, session: FirSession): Boolean = (symbol as? ConeClassLikeType)?.let {
        (it.toClassSymbol(session) as? FirRegularClassSymbol)?.let {
            it.classId == kotlinClassId("IntArray")
        }
    } ?: false

}

object SpecialTypes {

    val all = listOf(IntArray)


    fun lookup(symbol: ConeKotlinType, session: FirSession): SpecialType? =
        all.firstOrNull { it.match(symbol, session) }
}
