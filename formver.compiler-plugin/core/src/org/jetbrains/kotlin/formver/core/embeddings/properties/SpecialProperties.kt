/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.properties


import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.formver.core.conversion.CollectionSizeFieldEmbedding
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.kotlinCallableId
import org.jetbrains.kotlin.formver.core.names.NameMatcher
import org.jetbrains.kotlin.formver.core.names.embedMemberBackingFieldName
import org.jetbrains.kotlin.formver.core.names.embedName

abstract class SpecialProperty(val property: PropertyEmbedding) {
    context(typeResolver: TypeResolver, session: FirSession)
    abstract fun match(symbol: FirPropertySymbol) : Boolean
}


val StringSizeProperty = object: SpecialProperty(PropertyEmbedding(LengthFieldGetter, setter = null)) {
    context(typeResolver: TypeResolver, session: FirSession)
    override fun match(symbol: FirPropertySymbol) : Boolean {
        return symbol.callableId == kotlinCallableId("String", "length")
    }
}

val CollectionSizeProperty = object: SpecialProperty(PropertyEmbedding(BackingFieldGetter(CollectionSizeFieldEmbedding), setter = null)) {
    context(typeResolver: TypeResolver, session: FirSession)
    override fun match(symbol: FirPropertySymbol) : Boolean {
        val classSymbol = symbol.dispatchReceiverType?.toClassSymbol(session) as? FirRegularClassSymbol ?: return false

        val embedding = typeResolver.lookupClassTypeEmbedding(classSymbol.classId.embedName()) ?: return false
        val scopedName = symbol.callableId!!.embedMemberBackingFieldName(
            Visibilities.isPrivate(symbol.visibility)
        )
        NameMatcher.Companion.matchClassScope(scopedName) {
            ifBackingFieldName("size") {
                val result = typeResolver.isCollectionInheritor(embedding)
                return result
            }
            return false
        }
    }
}



object SpecialProperties {

    val all: List<SpecialProperty> = listOf(StringSizeProperty, CollectionSizeProperty)

    context(typeResolver: TypeResolver, session: FirSession)
    fun lookup(symbol: FirPropertySymbol) : PropertyEmbedding? = all.firstOrNull { it.match(symbol) }?.property

}
