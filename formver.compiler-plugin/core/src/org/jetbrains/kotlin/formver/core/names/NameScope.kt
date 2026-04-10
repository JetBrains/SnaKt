/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.AnyName
import org.jetbrains.kotlin.name.FqName

sealed interface NameScope : AnyName {
    val parent: NameScope?

    // Determines whether the parent should be part of the name.
    // This is a hack required by how we deal with public names.
    // We use only accessible scopes when generating the names, but all scopes when doing lookups
    // for things like package and class names.
    val parentAccessible: Boolean
        get() = true
}

// Includes the scope itself.
val NameScope.parentScopes: Sequence<NameScope>
    get() = sequence {
        if (parentAccessible) parent?.parentScopes?.let { yieldAll(it) }
        yield(this@parentScopes)
    }

val NameScope.allParentScopes: Sequence<NameScope>
    get() = sequence {
        parent?.parentScopes?.let { yieldAll(it) }
        yield(this@allParentScopes)
    }

val NameScope.packageNameIfAny: FqName?
    get() = allParentScopes.filterIsInstance<PackageScope>().lastOrNull()?.packageName

val NameScope.classNameIfAny: ClassKotlinName?
    get() = allParentScopes.filterIsInstance<ClassScope>().lastOrNull()?.className

data class PackageScope(val packageName: FqName) : NameScope {
    override val parent = null
}

data class ClassScope(override val parent: NameScope, val className: ClassKotlinName) : NameScope

/**
 * We do not want to mangle field names with class and package, hence introducing
 * this special `NameScope`. Note that it still needs package and class for other purposes.
 */
data class PublicScope(override val parent: NameScope) : NameScope {
    override val parentAccessible: Boolean
        get() = false
}

data class PrivateScope(override val parent: NameScope) : NameScope
data object ParameterScope : NameScope {
    override val parent: NameScope? = null
}

data object BadScope : NameScope {
    override val parent: NameScope? = null
}

data class LocalScope(val level: Int) : NameScope {
    override val parent: NameScope? = null
}

/**
 * Scope to use in cases when we need a scoped name, but don't actually want to introduce one.
 */
data object FakeScope : NameScope {
    override val parent: NameScope? = null
}
