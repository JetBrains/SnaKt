/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.formver.core.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.properties.BackingFieldGetter
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.properties.PropertyEmbedding
import org.jetbrains.kotlin.formver.core.linearization.pureToViper
import org.jetbrains.kotlin.formver.core.names.DispatchReceiverName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Predicate
import org.jetbrains.kotlin.utils.addIfNotNull

internal class ClassPredicateBuilder private constructor(
    val typeEmbedding: TypeEmbedding,
    val properties: List<PropertyEmbedding>,
    val classSuperTypes: List<ClassTypeEmbedding>
) {
    private val subject = PlaceholderVariableEmbedding(DispatchReceiverName, typeEmbedding)
    private val body = mutableListOf<ExpEmbedding>()

    companion object {
        context(ctx: TypeResolver)
        fun build(
            name: SymbolicName,
            predicateName: SymbolicName,
            action: ClassPredicateBuilder.() -> Unit,
        ): Predicate {
            val typeEmbedding = ctx.lookupClassTypeEmbedding(name)!!
            val builder = ClassPredicateBuilder(
                TypeEmbedding(typeEmbedding, TypeEmbeddingFlags(nullable = false)),
                ctx.lookupClassProperties(name),
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

    fun includeSubTypeInvariants() = body.add(
        SubTypeInvariantEmbedding(typeEmbedding).fillHole(subject)
    )

    fun forEachPropertyField(action: PropertyAssertionsBuilder.() -> Unit) =
        properties
            .forEach { property ->
                val builder = PropertyAssertionsBuilder(subject, property)
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

class PropertyAssertionsBuilder(private val subject: VariableEmbedding, private val property: PropertyEmbedding) {
    private val assertions = mutableListOf<ExpEmbedding>()
    fun toAssertionsList() = assertions.toList()

    val isUnique = property.isUnique

    context(ctx: TypeResolver)
    private fun getPlainValue() = when (val getter = property.getter!!) {
        is BackingFieldGetter -> PrimitiveFieldAccess(subject, getter.field)
        else -> getter.getValueSimple(subject, ctx)
    }


    context(ctx: TypeResolver)
    fun forType(action: TypeInvariantsBuilder.() -> Unit) {
        val builder = TypeInvariantsBuilder(property.type)
        builder.action()
        assertions.addAll(builder.toInvariantsList().fillHoles(getPlainValue()))
    }

    fun forBackingField(action: BackingFieldAssertionsBuilder.() -> Unit) {
        (property.getter as? BackingFieldGetter)?.field?.let { field ->
            val builder = BackingFieldAssertionsBuilder(subject, field)
            builder.action()
            assertions.addAll(builder.toAssertionsList())
        }
    }

    context(ctx: TypeResolver)
    fun addEqualsGuarantee(block: ExpEmbedding.() -> ExpEmbedding) {
        assertions.add(EqCmp(property.getter!!.getValue(subject, ctx), subject.block()))
    }
}

class BackingFieldAssertionsBuilder(private val subject: VariableEmbedding, private val field: FieldEmbedding) {
    private val assertions = mutableListOf<ExpEmbedding>()

    val isAlwaysWriteable = field.accessPolicy == AccessPolicy.ALWAYS_WRITEABLE

    fun toAssertionsList() = assertions.toList()

    fun addAccessPermissions(perm: PermExp) =
        assertions.add(FieldAccessTypeInvariantEmbedding(field, perm).fillHole(subject))


    fun forType(action: TypeInvariantsBuilder.() -> Unit) {
        val builder = TypeInvariantsBuilder(field.type)
        builder.action()
        assertions.addAll(builder.toInvariantsList().fillHoles(PrimitiveFieldAccess(subject, field)))
    }
}


class TypeInvariantsBuilder(private val type: TypeEmbedding) {
    private val invariants = mutableListOf<TypeInvariantEmbedding>()
    fun toInvariantsList() = invariants.toList()

    context(ctx: TypeResolver)
    fun addAccessToUniquePredicate() = invariants.addIfNotNull(
        type.uniquePredicateAccessInvariant(ctx)
    )

    fun includeSubTypeInvariants() = invariants.add(
        SubTypeInvariantEmbedding(type)
    )
}
