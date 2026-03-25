/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names_deprecated

import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.mangled
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse

sealed interface NameScope : SymbolicName {
    val parent: NameScope?

    context(nameResolver: NameResolver)
    val mangledScopeName: String?

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

context(nameResolver: NameResolver)
val NameScope.fullMangledName: String?
    get() {
        val scopes = parentScopes.mapNotNull { it.mangledScopeName }.toList()
        return if (scopes.isEmpty()) null else scopes.joinToString("$")
    }

val NameScope.packageNameIfAny: FqName?
    get() = allParentScopes.filterIsInstance<PackageScope>().lastOrNull()?.packageName

val NameScope.classNameIfAny: ClassKotlinName?
    get() = allParentScopes.filterIsInstance<ClassScope>().lastOrNull()?.className

data class PackageScope(val packageName: FqName) : NameScope {
    override val parent = null

    context(nameResolver: NameResolver)
    override val mangledScopeName: String?
        get() = packageName.isRoot.ifFalse { "pkg\$${packageName.asViperString()}" }

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = mangledScopeName ?: ""

    override fun dependsOn(): Set<SymbolicName> = setOfNotNull(parent)
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { "pkg\$${packageName.asViperString()}" }
    }
}

data class ClassScope(override val parent: NameScope, val className: ClassKotlinName) : NameScope {
    context(nameResolver: NameResolver)
    override val mangledScopeName: String
        get() = className.mangled

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = mangledScopeName

    override fun dependsOn(): Set<SymbolicName> = setOf(parent, className)
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { resolver -> resolver.resolve(className) }
        yield { resolver -> "${resolver.resolve(parent)}_${resolver.resolve(className)}" }
    }
}

/**
 * We do not want to mangle field names with class and package, hence introducing
 * this special `NameScope`. Note that it still needs package and class for other purposes.
 */
data class PublicScope(override val parent: NameScope) : NameScope {
    context(nameResolver: NameResolver)
    override val mangledScopeName: String
        get() = "public"
    override val parentAccessible: Boolean
        get() = false

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = mangledScopeName

    override fun dependsOn(): Set<SymbolicName> = setOf(parent)
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { "public" }
        yield { resolver -> "${resolver.resolve(parent)}_public" }
    }
}

data class PrivateScope(override val parent: NameScope) : NameScope {
    context(nameResolver: NameResolver)
    override val mangledScopeName: String
        get() = "private"

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = mangledScopeName

    override fun dependsOn(): Set<SymbolicName> = setOf(parent)
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { "private" }
        yield { resolver -> "${resolver.resolve(parent)}_private" }
    }
}

data object ParameterScope : NameScope {
    override val parent: NameScope? = null

    context(nameResolver: NameResolver)
    override val mangledScopeName: String
        get() = "p"

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = mangledScopeName

    override fun dependsOn(): Set<SymbolicName> = setOfNotNull(parent)
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { "p" }
    }
}

data object BadScope : NameScope {
    override val parent: NameScope? = null

    context(nameResolver: NameResolver)
    override val mangledScopeName: String
        get() = "<BAD>"

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = mangledScopeName

    override fun dependsOn(): Set<SymbolicName> = setOfNotNull(parent)
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { "<BAD>" }
    }
}

data class LocalScope(val level: Int) : NameScope {
    override val parent: NameScope? = null

    context(nameResolver: NameResolver)
    override val mangledScopeName: String
        get() = "l$level"

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = mangledScopeName

    override fun dependsOn(): Set<SymbolicName> = setOfNotNull(parent)
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { "l" }
        yield { "l$level" }
    }
}

/**
 * Scope to use in cases when we need a scoped name, but don't actually want to introduce one.
 */
data object FakeScope : NameScope {
    override val parent: NameScope? = null

    context(nameResolver: NameResolver)
    override val mangledScopeName: String?
        get() = null

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = mangledScopeName ?: ""

    override fun dependsOn(): Set<SymbolicName> = setOfNotNull(parent)
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { "" }
    }
}
