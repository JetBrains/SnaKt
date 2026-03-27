/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.*
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
    override val nameType: NameType?
        get() = name.nameType

    override val candidates: List<CandidateName>
        get() = buildCandidates {
            candidate {
                +name
            }
            candidate {
                +scope
                +name
            }
            if (nameType != null) {
                candidate {
                    +scope
                    +nameType!!
                    +name
                }
            }
        }
}

fun FqName.asViperString() = asString().replace('.', '_')

fun ScopedKotlinName.asScope(): NameScope {
    val className = name as? ClassKotlinName
    require(className != null) { "Only classes can be used for scopes." }
    return ClassScope(scope, className)
}