/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.types

import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.names.PredicateName
import org.jetbrains.kotlin.formver.core.names.ScopedName
import org.jetbrains.kotlin.formver.core.names.SimpleKotlinName
import org.jetbrains.kotlin.formver.core.names.asScope
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Predicate
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

class ClassEmbeddingDetails(
    val type: ClassTypeEmbedding,
) : TypeInvariantHolder {
    private var _superTypes: List<PretypeEmbedding>? = null
    val superTypes: List<PretypeEmbedding>
        get() = _superTypes ?: error("Super types of ${type.name} have not been initialised yet.")

    private val classSuperTypes: List<ClassTypeEmbedding>
        get() = superTypes.filterIsInstance<ClassTypeEmbedding>()

    fun initSuperTypes(newSuperTypes: List<PretypeEmbedding>) {
        check(_superTypes == null) { "Super types of ${type.name} are already initialised." }
        _superTypes = newSuperTypes
    }

    private var _fields: Map<SimpleKotlinName, FieldEmbedding>? = null
    private var _sharedPredicate: Predicate? = null
    private var _uniquePredicate: Predicate? = null
    val fields: Map<SimpleKotlinName, FieldEmbedding>
        get() = _fields ?: error("Fields of ${type.name} have not been initialised yet.")
    val sharedPredicate: Predicate
        get() = _sharedPredicate ?: error("Predicate of ${type.name} has not been initialised yet.")
    val uniquePredicate: Predicate
        get() = _uniquePredicate ?: error("Unique Predicate of ${type.name} has not been initialised yet.")

    fun initFields(newFields: Map<SimpleKotlinName, FieldEmbedding>) {
        check(_fields == null) { "Fields of ${type.name} are already initialised." }
        _fields = newFields
        _sharedPredicate = ClassPredicateBuilder.build(this, sharedPredicateName) {
            forEachField {
                if (isAlwaysReadable) {
                    addAccessPermissions(PermExp.WildcardPerm())
                    forType {
                        addAccessToSharedPredicate()
                        includeSubTypeInvariants()
                    }
                }
            }
            forEachSuperType {
                addAccessToSharedPredicate()
            }
        }
        _uniquePredicate = ClassPredicateBuilder.build(this, uniquePredicateName) {
            forEachField {
                if (isAlwaysReadable) {
                    addAccessPermissions(PermExp.WildcardPerm())
                } else {
                    addAccessPermissions(PermExp.FullPerm())
                }
                forType {
                    addAccessToSharedPredicate()
                    if (isUnique) {
                        addAccessToUniquePredicate()
                    }
                    includeSubTypeInvariants()
                }
            }
            forEachSuperType {
                addAccessToUniquePredicate()
            }
        }
    }

    private val sharedPredicateName = ScopedName(type.name.asScope(), PredicateName("shared"))
    private val uniquePredicateName = ScopedName(type.name.asScope(), PredicateName("unique"))

    /**
     * Find an embedding of a backing field by this name amongst the ancestors of this type.
     *
     * While in Kotlin only classes can have backing fields, and so searching interface supertypes is not strictly necessary,
     * due to the way we handle list size we need to search all types.
     */
    fun findField(name: SimpleKotlinName): FieldEmbedding? = fields[name]

    fun <R> flatMapFields(action: (SimpleKotlinName, FieldEmbedding) -> List<R>): List<R> =
        classSuperTypes.flatMap { it.details.flatMapFields(action) } + fields.flatMap { (name, field) ->
            action(
                name, field
            )
        }

    // We can't easily implement this by recursion on the supertype structure since some supertypes may be seen multiple times.
    // TODO: figure out a nicer way to handle this.
    override fun accessInvariants(): List<TypeInvariantEmbedding> =
        flatMapUniqueFields { _, field -> field.accessInvariantsForParameter() }

    // Note: this function will replace accessInvariants when nested unfold will be implemented
    override fun sharedPredicateAccessInvariant() =
        PredicateAccessTypeInvariantEmbedding(sharedPredicateName, PermExp.WildcardPerm())

    override fun uniquePredicateAccessInvariant() =
        PredicateAccessTypeInvariantEmbedding(uniquePredicateName, PermExp.FullPerm())

    override fun subTypeInvariant(): TypeInvariantEmbedding = type.subTypeInvariant()

    /**
     * Returns the sequence of types to unfold to reach [target] from the current type.
     * Returns an empty sequence if the current type already is [target].
     * Throws if [target] is unreachable.
     */
    fun hierarchyPathTo(target: ClassTypeEmbedding): Sequence<ClassTypeEmbedding> =
        pathToOrNull(target)?.asSequence()
            ?: throw IllegalArgumentException("Could not find a path from ${type.name} to ${target.name} in the class hierarchy")

    /**
     * Returns the path of types to unfold from the current type to reach [target], or `null` if unreachable.
     * An empty list means the current type IS the target.
     */
    private fun pathToOrNull(target: ClassTypeEmbedding): List<ClassTypeEmbedding>? {
        if (type == target) return emptyList()
        for (sup in classSuperTypes) {
            val subPath = sup.details.pathToOrNull(target) ?: continue
            return listOf(type) + subPath
        }
        return null
    }

    fun <R> flatMapUniqueFields(action: (SimpleKotlinName, FieldEmbedding) -> List<R>): List<R> {
        val seenFields = mutableSetOf<SimpleKotlinName>()
        return flatMapFields { name, field ->
            seenFields.add(name).ifTrue {
                action(name, field)
            } ?: listOf()
        }
    }
}
