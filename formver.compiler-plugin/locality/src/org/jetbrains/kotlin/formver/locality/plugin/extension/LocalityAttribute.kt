/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin.extension

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import kotlin.reflect.KClass

fun LocalityAttribute?.accepts(other: LocalityAttribute?): Boolean {
    return when (this) {
        null -> other == null
        else -> other == null || owner == other.owner
    }
}

data class LocalityAttribute(
    val owner: FirBasedSymbol<*>? = null
) : ConeAttribute<LocalityAttribute>() {
    override fun union(other: LocalityAttribute?): LocalityAttribute {
        if (other == null) return this

        return if (owner == other.owner) this else LocalityAttribute()
    }

    override fun intersect(other: LocalityAttribute?): LocalityAttribute? {
        if (other == null) return null

        return if (owner == other.owner) this else null
    }

    override fun add(other: LocalityAttribute?): LocalityAttribute = union(other)

    override fun isSubtypeOf(other: LocalityAttribute?): Boolean = other.accepts(this)

    override val key: KClass<out LocalityAttribute>
        get() = LocalityAttribute::class

    override val keepInInferredDeclarationType: Boolean
        get() = true
}

val ConeAttributes.locality: LocalityAttribute? by ConeAttributes.attributeAccessor<LocalityAttribute>()

fun LocalityAttribute?.union(other: LocalityAttribute?): LocalityAttribute? {
    if (this == null) return other

    return union(other)
}

val LocalityRenderer = Renderer<LocalityAttribute?> { locality ->
    when (locality) {
        null -> "'global'"
        else -> "'local(${(locality.owner as? FirCallableSymbol<*>)?.name ?: "unknown"})'"
    }
}
