/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.ConeAttributes
import kotlin.reflect.KClass

object LocalityAttribute : ConeAttribute<LocalityAttribute>() {
    override fun union(other: LocalityAttribute?): LocalityAttribute = this

    override fun intersect(other: LocalityAttribute?): LocalityAttribute? = other

    override fun add(other: LocalityAttribute?): LocalityAttribute = this

    override fun isSubtypeOf(other: LocalityAttribute?): Boolean = other != null

    override fun toString(): String = "LocalityAttribute"

    override val key: KClass<out LocalityAttribute>
        get() = LocalityAttribute::class

    override val keepInInferredDeclarationType: Boolean
        get() = true
}

val ConeAttributes.locality: LocalityAttribute? by ConeAttributes.attributeAccessor<LocalityAttribute>()
