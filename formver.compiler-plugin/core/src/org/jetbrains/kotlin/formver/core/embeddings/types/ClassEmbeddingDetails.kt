/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.names.PredicateName
import org.jetbrains.kotlin.formver.core.names.ScopedName
import org.jetbrains.kotlin.formver.core.names.asScope
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

class ClassEmbeddingDetails(
    val type: ClassTypeEmbedding,
    val fields: Map<SymbolicName, FieldEmbedding>,
    val classSuperTypes: List<ClassTypeEmbedding>,
    val isInterface: Boolean
) : TypeInvariantHolder {
    private val sharedPredicateName = ScopedName(type.name.asScope(), PredicateName("shared"))
    private val uniquePredicateName = ScopedName(type.name.asScope(), PredicateName("unique"))
    fun sharedPredicate(typeResolver: TypeResolver) =
        ClassPredicateBuilder.build(this, sharedPredicateName, typeResolver) {
            forEachField {
                if (isAlwaysReadable) {
                    addAccessPermissions(PermExp.WildcardPerm())
                    forType {
                        addAccessToSharedPredicate(typeResolver)
                        includeSubTypeInvariants()
                    }
                }
            }
            forEachSuperType {
                addAccessToSharedPredicate(typeResolver)
            }
        }

    fun uniquePredicate(typeResolver: TypeResolver) =
        ClassPredicateBuilder.build(this, uniquePredicateName, typeResolver) {
            forEachField {
                if (isAlwaysReadable) {
                    addAccessPermissions(PermExp.WildcardPerm())
                } else {
                    addAccessPermissions(PermExp.FullPerm())
                }
                forType {
                    addAccessToSharedPredicate(typeResolver)
                    if (isUnique) {
                        addAccessToUniquePredicate(typeResolver)
                    }
                    includeSubTypeInvariants()
                }
            }
            forEachSuperType {
                addAccessToUniquePredicate(typeResolver)
        }
    }


    /**
     * Find an embedding of a backing field by this name amongst the ancestors of this type.
     *
     * While in Kotlin only classes can have backing fields, and so searching interface supertypes is not strictly necessary,
     * due to the way we handle list size we need to search all types.
     */
    fun findField(name: SymbolicName): FieldEmbedding? = fields[name]

    fun <R> flatMapFields(ctx: TypeResolver, action: (SymbolicName, FieldEmbedding) -> List<R>): List<R> =
        classSuperTypes.flatMap { ctx.details(it.name).flatMapFields(ctx, action) } + fields.flatMap { (name, field) ->
            action(
                name, field
            )
        }

    // We can't easily implement this by recursion on the supertype structure since some supertypes may be seen multiple times.
    // TODO: figure out a nicer way to handle this.
    override fun accessInvariants(ctx: TypeResolver): List<TypeInvariantEmbedding> =
        flatMapUniqueFields(ctx) { _, field -> field.accessInvariantsForParameter() }

    // Note: this function will replace accessInvariants when nested unfold will be implemented
    override fun sharedPredicateAccessInvariant(ctx: TypeResolver) =
        PredicateAccessTypeInvariantEmbedding(sharedPredicateName, PermExp.WildcardPerm())

    override fun uniquePredicateAccessInvariant(ctx: TypeResolver) =
        PredicateAccessTypeInvariantEmbedding(uniquePredicateName, PermExp.FullPerm())

    override fun subTypeInvariant(): TypeInvariantEmbedding = type.subTypeInvariant()

    // Returns the sequence of classes in a hierarchy that need to be unfolded in order to access the given field
    fun hierarchyPathTo(field: FieldEmbedding, ctx: TypeResolver): Sequence<ClassTypeEmbedding> = sequence {
        val className = field.containingClass?.name
        require(className != null) { "Cannot find hierarchy unfold path of a field with no class information" }
        if (className == type.name) {
            yield(this@ClassEmbeddingDetails.type)
        } else {
            val sup = classSuperTypes.map { ctx.details(it.name) }.firstOrNull { !it.isInterface }
                ?: throw IllegalArgumentException("Reached top of the hierarchy without finding the field")

            yield(this@ClassEmbeddingDetails.type)
            yieldAll(sup.hierarchyPathTo(field, ctx))
        }
    }

    fun <R> flatMapUniqueFields(ctx: TypeResolver, action: (SymbolicName, FieldEmbedding) -> List<R>): List<R> {
        val seenFields = mutableSetOf<SymbolicName>()
        return flatMapFields(ctx) { name, field ->
            seenFields.add(name).ifTrue {
                action(name, field)
            } ?: listOf()
        }
    }
}
