/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.AnyName
import org.jetbrains.kotlin.formver.viper.CandidateName
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


data class PackageScope(val packageName: FqName) : NameScope {
    override val candidates: List<CandidateName> = buildCandidates {
        val split = packageName.asString().split(".")
        split.indices.forEach { i -> candidate { +split.takeLast(i + 1) } }
        candidate {
            +"pkg"
            +split
        }
    }
    override val children: List<AnyName> = emptyList()
}

data class ClassScope(override val parent: NameScope, val className: ClassKotlinName) : NameScope {
    override val candidates: List<CandidateName> = nameWithDependentPrefixCandidates(className, parent)
    override val children: List<AnyName> = listOf(parent, className)
}

data object PublicScope : NameScope {
    override val candidates: List<CandidateName> = nameOnlyCandidates("public")
    override val children: List<AnyName> = emptyList()
}

data class PrivateScope(override val parent: NameScope) : NameScope {
    override val candidates: List<CandidateName> = nameWithDependentPrefixCandidates("private", parent)
    override val children: List<AnyName> = listOf(parent)
}

data object ParameterScope : NameScope {
    override val candidates: List<CandidateName> = nameOnlyCandidates("par")
    override val children: List<AnyName> = emptyList()
}

data object BadScope : NameScope {
    override val candidates: List<CandidateName> = nameOnlyCandidates("<BAD>")
    override val children: List<AnyName> = emptyList()
}

data class LocalScope(val level: Int) : NameScope {
    override val candidates: List<CandidateName> = buildCandidates {
        candidateNoSeparator {
            +"l"
            +"$level"
        }
    }
    override val children: List<AnyName> = emptyList()
}

/**
 * Scope to use in cases when we need a scoped name, but don't actually want to introduce one.
 */
data object FakeScope : NameScope {
    override val candidates: List<CandidateName> = nameOnlyCandidates("fake")
    override val children: List<AnyName> = emptyList()
}
