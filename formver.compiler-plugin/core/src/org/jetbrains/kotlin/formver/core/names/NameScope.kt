/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.mangled
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse

/**
 * A single level in the scope chain used to produce globally unique Viper names for Kotlin entities.
 *
 * Implementations represent the different kinds of scopes that can appear in a Kotlin program:
 * package, class (public or private), function parameter, local variable, or synthetic helpers
 * such as [BadScope] and [FakeScope].
 *
 * The full mangled scope prefix for a [ScopedKotlinName] is assembled by walking
 * [parentScopes] and joining each non-null [mangledScopeName] with `$`.
 */
sealed interface NameScope {
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

/**
 * Returns a sequence of ancestor scopes ending with (and including) this scope,
 * honouring [NameScope.parentAccessible] so that inaccessible parents are omitted from
 * the generated Viper name prefix.
 */
// Includes the scope itself.
val NameScope.parentScopes: Sequence<NameScope>
    get() = sequence {
        if (parentAccessible) parent?.parentScopes?.let { yieldAll(it) }
        yield(this@parentScopes)
    }

/**
 * Like [parentScopes] but always includes every ancestor regardless of
 * [NameScope.parentAccessible], used for structural lookups such as finding
 * the enclosing package or class name.
 */
val NameScope.allParentScopes: Sequence<NameScope>
    get() = sequence {
        parent?.parentScopes?.let { yieldAll(it) }
        yield(this@allParentScopes)
    }

/**
 * Assembles the full `$`-separated mangled scope prefix for this scope by joining
 * all non-null [NameScope.mangledScopeName] values from [parentScopes].
 * Returns `null` if none of the accessible scope levels contribute a name.
 */
context(nameResolver: NameResolver)
val NameScope.fullMangledName: String?
    get() {
        val scopes = parentScopes.mapNotNull { it.mangledScopeName }.toList()
        return if (scopes.isEmpty()) null else scopes.joinToString("$")
    }

/**
 * Returns the [FqName] of the innermost enclosing [PackageScope], or `null` if this
 * scope chain does not contain a package component.
 */
val NameScope.packageNameIfAny: FqName?
    get() = allParentScopes.filterIsInstance<PackageScope>().lastOrNull()?.packageName

/**
 * Returns the [ClassKotlinName] of the innermost enclosing [ClassScope], or `null` if
 * this scope chain does not contain a class component.
 */
val NameScope.classNameIfAny: ClassKotlinName?
    get() = allParentScopes.filterIsInstance<ClassScope>().lastOrNull()?.className

/**
 * Scope level representing a Kotlin package.  Contributes a `pkg$<package>` prefix to
 * the mangled name unless the package is the root package, in which case it contributes nothing.
 */
data class PackageScope(val packageName: FqName) : NameScope {
    override val parent = null

    context(nameResolver: NameResolver)
    override val mangledScopeName: String?
        get() = packageName.isRoot.ifFalse { "pkg\$${packageName.asViperString()}" }
}

/**
 * Scope level representing a Kotlin class or object declaration.
 * Contributes the mangled class name to the Viper name prefix.
 */
data class ClassScope(override val parent: NameScope, val className: ClassKotlinName) : NameScope {
    context(nameResolver: NameResolver)
    override val mangledScopeName: String
        get() = className.mangled
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
}

/**
 * Scope level for private Kotlin declarations.
 * Contributes a `private` segment to the mangled name.
 */
data class PrivateScope(override val parent: NameScope) : NameScope {
    context(nameResolver: NameResolver)
    override val mangledScopeName: String
        get() = "private"
}

/**
 * Scope level used for function value parameters.
 * Contributes a short `p` prefix to the mangled name, keeping parameter names compact.
 */
data object ParameterScope : NameScope {
    override val parent: NameScope? = null

    context(nameResolver: NameResolver)
    override val mangledScopeName: String
        get() = "p"
}

/**
 * Sentinel scope used as a placeholder when name resolution has failed or is not yet
 * implemented.  Contributes the literal string `<BAD>` to the mangled name so that
 * such names are visually conspicuous in debug output.
 */
data object BadScope : NameScope {
    override val parent: NameScope? = null

    context(nameResolver: NameResolver)
    override val mangledScopeName: String
        get() = "<BAD>"
}

/**
 * Scope level for local variables, distinguished by a nesting [level] counter.
 * Contributes an `l<n>` prefix (e.g. `l0`, `l1`) so that variables at different
 * nesting depths do not collide in the generated Viper name.
 */
data class LocalScope(val level: Int) : NameScope {
    override val parent: NameScope? = null

    context(nameResolver: NameResolver)
    override val mangledScopeName: String
        get() = "l$level"
}

/**
 * Scope to use in cases when we need a scoped name, but don't actually want to introduce one.
 */
data object FakeScope : NameScope {
    override val parent: NameScope? = null

    context(nameResolver: NameResolver)
    override val mangledScopeName: String?
        get() = null
}
