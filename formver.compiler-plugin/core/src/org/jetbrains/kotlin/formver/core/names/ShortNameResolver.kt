package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.viper.*
import kotlin.math.absoluteValue

/**
 * Resolves mangled names into Viper identifiers while maintaining uniqueness.
 * The priority lies on the short and readable names.
 */
class ShortNameResolver : NameResolver {
    // DAG Datastructures

    val graph = NameSystemGraph()

    override fun resolve(name: SymbolicName): String =
        listOfNotNull(name.nameType, name.mangledScope, name.mangledBaseName).joinToString(SEPARATOR)

    override fun register(name: SymbolicName) {
        graph.addName(name)
    }


}

class NameSystemGraph {

    // contains a hierarchy of scopes
    private val scopeRelation = mutableMapOf<NameScope, Set<NameScope>>()

    // contains mapping from scopes to their names
    private val scopedNameRelation = mutableMapOf<NameScope, Set<KotlinName>>()

    // collects the names that belong to one type
    private val nameTypes = mutableMapOf<NameType, Set<SymbolicName>>()

    private val kotlinNames = mutableMapOf<KotlinName, Set<KotlinName>>()

    private val freshNames = mutableMapOf<SymbolicName, Set<SymbolicName>>()

    private val endUpInViper = mutableSetOf<NamedEntity>()

    fun normalize() {
        scopeRelation.values.flatten().forEach { scope ->
            scopeRelation.merge(scope, emptySet(), Set<NameScope>::plus)
        }
        scopedNameRelation.values.flatten()
            .forEach { name -> kotlinNames.merge(name, emptySet(), Set<KotlinName>::plus) }
        nameTypes.values.flatten().forEach { name -> freshNames.merge(name, emptySet(), Set<SymbolicName>::plus) }
        kotlinNames.values.flatten().forEach { name -> kotlinNames.merge(name, emptySet(), Set<KotlinName>::plus) }
        freshNames.values.flatten().forEach { name -> freshNames.merge(name, emptySet(), Set<SymbolicName>::plus) }
    }

    fun addScope(scope: NameScope) {
        scopeRelation.merge(scope, emptySet(), Set<NameScope>::plus)
        if (scope.parent != null) {
            scopeRelation.merge(
                scope.parent!!,
                setOf(scope),
                Set<NameScope>::plus,
            )
            addScope(scope.parent!!)
        }
    }

    fun addScopedName(scope: NameScope, name: KotlinName) {
        addScope(scope)
        scopedNameRelation.merge(
            scope,
            setOf(name),
            Set<KotlinName>::plus,
        )
        addKotlinName(name)
    }

    fun addTypedKotlinName(name: TypedKotlinName) {
        addNameWithType(name)
    }

    fun addNameWithType(name: SymbolicName) {
        if (name.nameType != null) {
            nameTypes.merge(name.nameType!!, setOf(name), Set<SymbolicName>::plus)
        }
    }

    fun addKotlinName(name: KotlinName) {
        addNameWithType(name)
        when (name) {
            is SimpleKotlinName -> {
                kotlinNames.merge(name, emptySet(), Set<KotlinName>::plus)
            }

            is TypedKotlinName -> {
                addTypedKotlinName(name)
            }

            is TypedKotlinNameWithType -> {
                kotlinNames.merge(name.type.name, setOf(name), Set<KotlinName>::plus)
                kotlinNames.merge(name, emptySet(), Set<KotlinName>::plus)
            }

            is ClassKotlinName -> {
                kotlinNames.merge(name, emptySet(), Set<KotlinName>::plus)
            }

            is ConstructorKotlinName -> {
                kotlinNames.merge(name, emptySet(), Set<KotlinName>::plus)
            }

            is PredicateKotlinName -> {
                kotlinNames.merge(name, emptySet(), Set<KotlinName>::plus)
            }

            is HavocKotlinName -> {
                kotlinNames.merge(name, emptySet(), Set<KotlinName>::plus)
            }

            is PretypeName -> {
                kotlinNames.merge(name, emptySet(), Set<KotlinName>::plus)
            }

            is ListOfNames<*> -> {
                name.names.forEach {
                    kotlinNames.merge(it as KotlinName, setOf(name), Set<KotlinName>::plus)
                }
                addKotlinName(name)
            }

            is FunctionTypeName -> {
                kotlinNames.merge(name.args, setOf(name), Set<KotlinName>::plus)
                kotlinNames.merge(name.returns, setOf(name), Set<KotlinName>::plus)
                kotlinNames.merge(name, emptySet(), Set<KotlinName>::plus)
            }

            is TypeName -> {
                kotlinNames.merge(name, emptySet(), Set<KotlinName>::plus)
                freshNames.merge(name.pretype.name, setOf(name), Set<SymbolicName>::plus)
            }
        }

    }

    fun addName(name: SymbolicName) {
        when (name) {
            is ScopedKotlinName -> {
                val parent = name.scope
                val child = name.name
                addScopedName(parent, child)
                addKotlinName(child)
                return
            }

            is KotlinName -> addKotlinName(name)

            is AnonymousName -> {
                freshNames.merge(name, emptySet<SymbolicName>(), Set<SymbolicName>::plus)
            }

            is AnonymousBuiltinName -> {
                freshNames.merge(name, emptySet(), Set<SymbolicName>::plus)
            }

            is PlaceholderReturnVariableName -> {
                freshNames.merge(name, emptySet(), Set<SymbolicName>::plus)
            }


            is ReturnVariableName -> {
                freshNames.merge(name, emptySet(), Set<SymbolicName>::plus)
            }


            is FunctionResultVariableName -> {
                freshNames.merge(name, emptySet(), Set<SymbolicName>::plus)
            }


            is DispatchReceiverName -> {
                freshNames.merge(name, emptySet(), Set<SymbolicName>::plus)
            }


            is ExtensionReceiverName -> {
                freshNames.merge(name, emptySet(), Set<SymbolicName>::plus)
            }


            is NumberedLabelName -> {
                freshNames.merge(name, emptySet(), Set<SymbolicName>::plus)
            }

            is PlaceholderArgumentName -> {
                freshNames.merge(name, emptySet(), Set<SymbolicName>::plus)
            }

            is SsaVariableName -> {
                freshNames.merge(name.baseName, setOf(name), Set<SymbolicName>::plus)
                freshNames.merge(name, emptySet(), Set<SymbolicName>::plus)
            }

            else -> {
                // This excludes the whole type domain
                return
            }
        }
        addNameWithType(name)
    }

    context(resolver: NameResolver)
    fun toGraphviz(): String {
//        normalize()
        val sb = StringBuilder()
        sb.append("digraph NameSystem {\n")
        sb.append("  node [shape=box];\n")

        // Helper to get a unique ID for each node
        fun id(obj: NamedEntity): String {
            val hashCode = obj.hashCode() + obj::class.qualifiedName.hashCode()
            val name = "n${hashCode.absoluteValue}"
            return name
        }

        // Helper for labels
        fun label(obj: NamedEntity): String = when (obj) {
            is NameScope -> "Scope: ${obj.name()}"
            is SymbolicName -> "Name: ${obj.name()}"
            is NameType -> "Type: ${obj.name()}"
            else -> throw IllegalArgumentException("Unsupported entity type: ${obj.javaClass.name}")
        }

        // 1. Scopes
        sb.append("  subgraph cluster_scopes {\n")
        sb.append("    label=\"Scopes\";\n")
        sb.append("    node [color=blue, style=filled, fillcolor=lightblue];\n")
        scopeRelation.keys.forEach { scope ->
            sb.append("    ${id(scope)} [label=\"${label(scope)}\"];\n")
        }
        sb.append("  }\n")

        // 2. Types
        sb.append("  subgraph cluster_types {\n")
        sb.append("    label=\"Types\";\n")
        sb.append("    node [color=green, style=filled, fillcolor=lightgreen, shape=ellipse];\n")
        nameTypes.keys.forEach { type ->
            sb.append("    ${id(type)} [label=\"${label(type)}\"];\n")
        }
        sb.append("  }\n")

        // 3. Names
        sb.append("  subgraph cluster_names {\n")
        sb.append("    label=\"Names\";\n")
        sb.append("    node [color=orange, style=filled, fillcolor=lightsalmon];\n")
        val allNames = (scopedNameRelation.values.flatten() + nameTypes.values.flatten() +
                kotlinNames.keys + freshNames.keys + freshNames.values.flatten()).toSet()
        allNames.forEach { name ->
            sb.append("    ${id(name)} [label=\"${label(name)}\"];\n")
        }
        sb.append("  }\n")

        // Relations
        // Scope hierarchy: parent -> child
        scopeRelation.forEach { (parent, children) ->
            children.forEach { child ->
                sb.append("  ${id(parent)} -> ${id(child)} [label=\"parentOf\", color=blue, arrowhead=vee];\n")
            }
        }

        // Scope to names: scope -> name
        scopedNameRelation.forEach { (scope, names) ->
            names.forEach { name ->
                sb.append("  ${id(scope)} -> ${id(name)} [label=\"contains\", color=darkgreen, arrowhead=dot];\n")
            }
        }

        // Type to names: type -> name
        nameTypes.forEach { (type, names) ->
            names.forEach { name ->
                sb.append("  ${id(type)} -> ${id(name)} [label=\"isTypeOf\", color=green, arrowhead=normal];\n")
            }
        }

        // SSA/Fresh relations: base -> version
        freshNames.forEach { (base, versions) ->
            versions.forEach { version ->
                sb.append("  ${id(base)} -> ${id(version)} [label=\"ssa\", color=red, arrowhead=diamond];\n")
            }
        }

        // KotlinName relations (e.g., ListOfNames -> components)
        kotlinNames.forEach { (child, parents) ->
            parents.forEach { parent ->
                sb.append("  ${id(parent)} -> ${id(child)} [label=\"memberOf\", color=purple, arrowhead=inv];\n")
            }
        }

        sb.append("}\n")
        return sb.toString()
    }
}


