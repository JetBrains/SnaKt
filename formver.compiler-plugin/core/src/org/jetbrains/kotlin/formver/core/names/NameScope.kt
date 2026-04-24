/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.AnyName
import org.jetbrains.kotlin.name.FqName

sealed interface NameScope : AnyName {
    val parent: NameScope?
        get() = null

    override val inViper: Boolean
        get() = true
}

// Includes the scope itself.
val NameScope.parentScopes: Sequence<NameScope>
    get() = sequence {
        parent?.parentScopes?.let { yieldAll(it) }
        yield(this@parentScopes)
    }

val NameScope.allParentScopes: Sequence<NameScope>
    get() = sequence {
        parent?.parentScopes?.let { yieldAll(it) }
        yield(this@allParentScopes)
    }

val NameScope.packageNameIfAny: FqName?
    get() = allParentScopes.filterIsInstance<PackageScope>().lastOrNull()?.packageName


data class PackageScope(val packageName: FqName) : NameScope

data class ClassScope(override val parent: NameScope, val className: ClassKotlinName) : NameScope

data object PublicScope : NameScope

data class PrivateScope(override val parent: NameScope) : NameScope

data object ParameterScope : NameScope

data object BadScope : NameScope

data class LocalScope(val level: Int) : NameScope

/**
 * Scope to use in cases when we need a scoped name, but don't actually want to introduce one.
 */
data object FakeScope : NameScope
