/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.name.FqName

/**
 * Name of a Kotlin entity in the original program in a specified scope and optionally distinguished by type.
 */
data class ScopedKotlinName(val scope: NameScope, val name: KotlinName) : SymbolicName {
    context(nameResolver: NameResolver)
    override val mangledScope: String?
        get() = scope.fullMangledName

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = name.mangledBaseName
    override val mangledType: String?
        get() = name.mangledType
}

/**
 * Converts a fully-qualified Kotlin name to a Viper-compatible string by replacing
 * every `.` separator with `_`.
 */
fun FqName.asViperString() = asString().replace('.', '_')

/**
 * Promotes this [ScopedKotlinName] into a [NameScope] so that it can be used as the
 * enclosing scope for members of the named class.
 *
 * Only valid when the underlying [KotlinName] is a [ClassKotlinName]; throws
 * [IllegalArgumentException] otherwise.
 */
fun ScopedKotlinName.asScope(): NameScope {
    val className = name as? ClassKotlinName
    require(className != null) { "Only classes can be used for scopes." }
    return ClassScope(scope, className)
}