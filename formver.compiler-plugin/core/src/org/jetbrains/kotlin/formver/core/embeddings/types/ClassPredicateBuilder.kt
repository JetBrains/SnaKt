/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.formver.core.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.properties.UserFieldEmbedding
import org.jetbrains.kotlin.formver.core.linearization.pureToViper
import org.jetbrains.kotlin.formver.core.names.DispatchReceiverName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Predicate
import org.jetbrains.kotlin.utils.addIfNotNull

internal class ClassPredicateBuilder private constructor(
    typeEmbedding: TypeEmbedding,
    val fields: List<FieldEmbedding>,
    val classSuperTypes: List<ClassTypeEmbedding>
) {
    private val subject = PlaceholderVariableEmbedding(DispatchReceiverName, typeEmbedding)
    private val body = mutableListOf<ExpEmbedding>()

    companion object {
        fun build(
            name: SymbolicName,
            predicateName: SymbolicName,
            ctx: TypeResolver,
            action: ClassPredicateBuilder.() -> Unit,
        ): Predicate {
            val typeEmbedding = ctx.lookupClassType(name)!!
            val builder = ClassPredicateBuilder(
                TypeEmbedding(typeEmbedding, TypeEmbeddingFlags(nullable = false)),
                ctx.lookupClassFields(name),
                ctx.lookupSuperTypes(name)
            )
            builder.action()
            return Predicate(
                predicateName,
                listOf(builder.subject.toLocalVarDecl()),
                builder.body.toConjunction().pureToViper(toBuiltin = true, ctx)
            )
        }
    }

    fun forEachField(action: FieldAssertionsBuilder.() -> Unit) =
        fields
            .filterIsInstance<UserFieldEmbedding>()
            .forEach { field ->
                val builder = FieldAssertionsBuilder(subject, field)
                builder.action()
                body.addAll(builder.toAssertionsList())
            }

    fun forEachSuperType(action: TypeInvariantsBuilder.() -> Unit) =
        classSuperTypes.forEach { type ->
            val builder = TypeInvariantsBuilder(type.asTypeEmbedding())
            builder.action()
            body.addAll(builder.toInvariantsList().fillHoles(subject))
        }
}

class FieldAssertionsBuilder(private val subject: VariableEmbedding, private val field: UserFieldEmbedding) {
    private val assertions = mutableListOf<ExpEmbedding>()
    fun toAssertionsList() = assertions.toList()

    val isAlwaysReadable = field.accessPolicy == AccessPolicy.ALWAYS_READABLE
    val isUnique = field.isUnique

    fun forType(action: TypeInvariantsBuilder.() -> Unit) {
        val builder = TypeInvariantsBuilder(field.type)
        builder.action()
        assertions.addAll(builder.toInvariantsList().fillHoles(PrimitiveFieldAccess(subject, field)))
    }

    fun addAccessPermissions(perm: PermExp) =
        assertions.add(FieldAccessTypeInvariantEmbedding(field, perm).fillHole(subject))

    fun addEqualsGuarantee(block: ExpEmbedding.() -> ExpEmbedding) {
        assertions.add(FieldEqualsInvariant(field, subject.block()).fillHole(subject))
    }
}

class TypeInvariantsBuilder(private val type: TypeEmbedding) {
    private val invariants = mutableListOf<TypeInvariantEmbedding>()
    fun toInvariantsList() = invariants.toList()

    fun addAccessToSharedPredicate(ctx: TypeResolver) = invariants.addIfNotNull(
        type.sharedPredicateAccessInvariant(ctx)
    )

    fun addAccessToUniquePredicate(ctx: TypeResolver) = invariants.addIfNotNull(
        type.uniquePredicateAccessInvariant(ctx)
    )

    fun includeSubTypeInvariants() = invariants.add(
        SubTypeInvariantEmbedding(type)
    )
}
