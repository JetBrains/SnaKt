/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.formver.core.domains.Injection
import org.jetbrains.kotlin.formver.core.domains.MethodBuilder
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain.Companion.subtype
import org.jetbrains.kotlin.formver.core.embeddings.expression.PlaceholderVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.PlaintextLeaf
import org.jetbrains.kotlin.formver.core.embeddings.expression.debug.TreeView
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.linearization.pureToViper
import org.jetbrains.kotlin.formver.core.names.HavocKotlinName
import org.jetbrains.kotlin.formver.core.names.PlaceholderReturnVariableName
import org.jetbrains.kotlin.formver.core.names.TypeName
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Method
import org.jetbrains.kotlin.formver.viper.ast.Type
import org.jetbrains.kotlin.formver.viper.mangled

/**
 * Represents our representation of a Kotlin type.
 *
 * Due to name mangling, the mapping between Kotlin types and TypeEmbeddings must be 1:1.
 */
data class TypeEmbedding(val pretype: PretypeEmbedding, val flags: TypeEmbeddingFlags) : RuntimeTypeHolder,
    TypeInvariantHolder {
    /**
     * Name representing the type, used for distinguishing overloads.
     *
     * It may at some point necessary to make a `TypeName` hierarchy of some sort to
     * represent these names, but we do it inline for now.
     */
    val name: SymbolicName
        get() = TypeName(pretype, flags.nullable)

    val havocMethodName: SymbolicName by lazy {
        HavocKotlinName(this)
    }

    val havocMethod: Method by lazy {
        MethodBuilder.build(havocMethodName) {
            returns {
                Type.Ref
            }
            sharedPredicateAccessInvariant()?.let {
                postcondition {
                    it.fillHole(
                        PlaceholderVariableEmbedding(
                            PlaceholderReturnVariableName,
                            this@TypeEmbedding.pretype.asTypeEmbedding()
                        )
                    ).pureToViper(toBuiltin = true)
                }
            }
            postcondition {
                RuntimeTypeDomain.typeOf(Exp.LocalVar(PlaceholderReturnVariableName, Type.Ref))
                    .subtype(this@TypeEmbedding.runtimeType)
            }
        }
    }

    /**
     * Get a nullable version of this type embedding.
     *
     * Note that nullability doesn't stack, hence nullable types must return themselves.
     */
    fun getNullable(): TypeEmbedding = copy(flags = flags.copy(nullable = true))

    /**
     * Drop nullability, if it is present.
     */
    fun getNonNullable(): TypeEmbedding = copy(flags = flags.copy(nullable = false))

    val isNullable: Boolean
        get() = flags.nullable

    override val runtimeType: Exp = flags.adjustRuntimeType(pretype.runtimeType)

    override fun accessInvariants(): List<TypeInvariantEmbedding> =
        flags.adjustManyInvariants(pretype.accessInvariants())

    override fun pureInvariants(): List<TypeInvariantEmbedding> = flags.adjustManyInvariants(pretype.pureInvariants())

    override fun sharedPredicateAccessInvariant(): TypeInvariantEmbedding? =
        flags.adjustOptionalInvariant(pretype.sharedPredicateAccessInvariant())

    override fun uniquePredicateAccessInvariant(): TypeInvariantEmbedding? =
        flags.adjustOptionalInvariant(pretype.uniquePredicateAccessInvariant())

    override fun subTypeInvariant(): TypeInvariantEmbedding = SubTypeInvariantEmbedding(this)

    context(nameResolver: NameResolver)
    override val debugTreeView: TreeView
        get() = PlaintextLeaf(name.mangled)

    fun hierarchyPathTo(field: FieldEmbedding): Sequence<ClassTypeEmbedding>? =
        // TODO: Find a nicer solution to avoid this cast. It should really be: type.hierarchyPathTo(field)
        (pretype as? ClassTypeEmbedding)?.details?.hierarchyPathTo(field)
}

data class TypeEmbeddingFlags(val nullable: Boolean) {
    fun adjustRuntimeType(runtimeType: Exp): Exp =
        if (nullable) RuntimeTypeDomain.nullable(runtimeType)
        else runtimeType

    fun adjustInvariant(invariant: TypeInvariantEmbedding): TypeInvariantEmbedding =
        if (nullable) IfNonNullInvariant(invariant)
        else invariant

    fun adjustManyInvariants(invariants: List<TypeInvariantEmbedding>): List<TypeInvariantEmbedding> =
        invariants.map { adjustInvariant(it) }

    fun adjustOptionalInvariant(invariant: TypeInvariantEmbedding?): TypeInvariantEmbedding? =
        invariant?.let { adjustInvariant(it) }
}

inline fun TypeEmbedding.injectionOr(default: (TypeEmbedding) -> Injection) = injectionOrNull ?: default(this)

val TypeEmbedding.injection
    get() = injectionOr {
        error("Type ${it.name} has no injection specified.")
    }

val TypeEmbedding.injectionOrNull: Injection?
    get() =
        if (flags.nullable) null
        else when (this.pretype) {
            StringTypeEmbedding -> RuntimeTypeDomain.stringInjection
            CharTypeEmbedding -> RuntimeTypeDomain.charInjection
            IntTypeEmbedding -> RuntimeTypeDomain.intInjection
            BooleanTypeEmbedding -> RuntimeTypeDomain.boolInjection
            else -> null
        }


