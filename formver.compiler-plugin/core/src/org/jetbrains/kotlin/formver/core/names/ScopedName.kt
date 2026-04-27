/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names


import org.jetbrains.kotlin.formver.core.names.shortNameResolver.buildCandidates
import org.jetbrains.kotlin.formver.core.names.shortNameResolver.nameWithDependentPrefixCandidates
import org.jetbrains.kotlin.formver.viper.AnyName
import org.jetbrains.kotlin.formver.viper.CandidateName
import org.jetbrains.kotlin.formver.viper.NameTypeBase
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.name.FqName

/**
 * Name of an entity specified by a scope and name.
 */
data class ScopedName(val scope: NameScope, val name: SymbolicName) : SymbolicName {

    override val nameType: NameTypeBase? = name.nameType
    override val inViper: Boolean = name !is ClassKotlinName

    override val candidates: List<CandidateName> = buildCandidates {
        +nameWithDependentPrefixCandidates(name, scope)
        if (nameType != null) {
            candidate {
                +nameType
                +scope
                +name
            }
        }
    }

    override val children: List<AnyName> = listOf(scope, name)
}

fun FqName.asViperString() = asString().replace('.', '_')

fun ScopedName.asScope(): NameScope {
    val className = name as? ClassKotlinName
    require(className != null) { "Only classes can be used for scopes." }
    return ClassScope(scope, className)
}
