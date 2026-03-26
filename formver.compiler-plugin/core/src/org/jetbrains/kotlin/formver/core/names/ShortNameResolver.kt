package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.NameType
import org.jetbrains.kotlin.formver.viper.SEPARATOR
import org.jetbrains.kotlin.formver.viper.SymbolicName

/**
 * Resolves mangled names into Viper identifiers while maintaining uniqueness.
 * The priority lies on the short and readable names.
 */
class ShortNameResolver : NameResolver {
    // DAG Datastructures



    override fun resolve(name: SymbolicName): String =
        listOfNotNull(name.nameType, name.mangledScope, name.mangledBaseName).joinToString(SEPARATOR)

    override fun register(name: SymbolicName) {}


}

class NameSystemGraph {

    // contains a hierarchy of scopes
    private val scopeRelation = mutableMapOf<NameScope, Set<NameScope>>()

    // contains mapping from scopes to their names
    private val scopedNameRelation = mutableMapOf<NameScope, Set<SymbolicName>>()

    // collects the names that belong to one type
    private val nameTypes = mutableMapOf<NameType, Set<SymbolicName>>()

    private val names = mutableMapOf<KotlinName, Set<KotlinName>>()

    fun addScope(scope: NameScope) {
        scopeRelation.merge(scope, emptySet(), Set<NameScope>::plus)
        if (scope.parent != null) {
            scopeRelation.merge(
                scope.parent!!,
                setOf(scope),
                Set<NameScope>::plus,
            )
        }
    }

    fun addScopedName(scope: NameScope, name: SymbolicName) =
        scopedNameRelation.merge(
            scope,
            setOf(name),
            Set<SymbolicName>::plus,
        )

    fun addSymbolicName(name: SymbolicName) {
        name.nameType?.let { nameTypes.merge(it, setOf(name), Set<SymbolicName>::plus) }
    }

    fun addTypedKotlinName(name: TypedKotlinName) {

    }

    fun addKotlinName(name: KotlinName) {
        when (name) {
            is SimpleKotlinName -> {
                // already handled
                names.merge(name, emptySet(), Set<KotlinName>::plus)
            }

            is TypedKotlinName -> addTypedKotlinName(name)
            else -> throw IllegalArgumentException("Unsupported KotlinName type: ${name::class.simpleName}")
        }

    }

    fun addName(name: SymbolicName) {
        when (name) {
            is ScopedKotlinName -> {
                val parent = name.scope
                val child = name.name
                addScopedName(parent, child)
                addKotlinName(child)
            }

            is KotlinName -> {

            }

        }
    }
}


