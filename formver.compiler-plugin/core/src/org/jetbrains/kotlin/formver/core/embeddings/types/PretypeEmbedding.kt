/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.PlaintextLeaf
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.TreeView
import org.jetbrains.kotlin.formver.core.names.PretypeName
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.mangled

/**
 * A representation of a Kotlin type without nullability and uniqueness information.
 *
 * We explicitly choose not to make this a subtype of `TypeEmbedding`, even though there is a simple way of treating
 * every `PretypeEmbedding` as a `TypeEmbedding`: the goal of the separation into types and pretypes is to avoid
 * one showing up where the other is expected.  For example, the naming systems are different, and the equality
 * comparisons would not work.
 *
 * All pretype embeddings must be `data` classes or objects!
 *
 * Pretypes do not implement [TypeInvariantHolder]: invariant queries are only meaningful once nullability
 * is known, so they live exclusively on [TypeEmbedding]. The pretype-side raw building blocks are exposed
 * through the package-private [rawAccessInvariants], [rawPureInvariants] and
 * [rawUniquePredicateAccessInvariant] helpers below, and only [TypeEmbedding] should call them.
 */
sealed interface PretypeEmbedding : RuntimeTypeHolder {
    val name: SymbolicName

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = PlaintextLeaf(name.mangled)
}

data object UnitTypeEmbedding : PretypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.unitType()
    override val name = PretypeName("Unit")
}

data object NothingTypeEmbedding : PretypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.nothingType()
    override val name = PretypeName("Nothing")
}

data object AnyTypeEmbedding : PretypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.anyType()
    override val name = PretypeName("Any")
}

data object IntTypeEmbedding : PretypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.intType()
    override val name = PretypeName("Int")
}

data object BooleanTypeEmbedding : PretypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.boolType()
    override val name = PretypeName("Boolean")
}

data object CharTypeEmbedding : PretypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.charType()
    override val name = PretypeName("Char")
}

data object StringTypeEmbedding : PretypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.stringType()
    override val name = PretypeName("String")
}

fun PretypeEmbedding.asTypeEmbedding() = TypeEmbedding(this, nullable = false)

internal fun PretypeEmbedding.rawAccessInvariants(ctx: TypeResolver): List<TypeInvariantEmbedding> = when (this) {
    is ClassTypeEmbedding -> ctx.flatMapUniqueFields(name) { field -> field.accessInvariantsForParameter() }
    else -> emptyList()
}

internal fun PretypeEmbedding.rawPureInvariants(): List<TypeInvariantEmbedding> = when (this) {
    NothingTypeEmbedding -> listOf(FalseTypeInvariant)
    else -> emptyList()
}

internal fun PretypeEmbedding.rawUniquePredicateAccessInvariant(): TypeInvariantEmbedding? = when (this) {
    is ClassTypeEmbedding -> PredicateAccessTypeInvariantEmbedding(uniquePredicateName, PermExp.FullPerm())
    else -> null
}
