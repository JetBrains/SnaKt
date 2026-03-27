package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.viper.*
import org.jetbrains.kotlin.formver.viper.ast.DomainName
import org.jetbrains.kotlin.formver.viper.ast.NamedDomainAxiomLabel
import org.jetbrains.kotlin.formver.viper.ast.QualifiedDomainFuncName
import org.jetbrains.kotlin.formver.viper.ast.UnqualifiedDomainFuncName
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

enum class Relation {
    /** scope SCOPE_OF scopedName**/
    SCOPE_OF,

    /** name SCOPED_NAME scopeNamed **/
    SCOPED_NAME,

    /** Function HAS_TYPE Int**/
    HAS_TYPE,

    /** (a,b) -> c DEPENDS_ON a **/
    DEPENDS_ON,

    /** name NAME_TYPE nameType **/
    NAME_TYPE,
}

class NameSystemGraph {

    // contains a hierarchy of scopes
    private val scopeRelation = mutableMapOf<NameScope, Set<NameScope>>()

    // contains mapping from scopes to their names
    private val scopedNameRelation = mutableMapOf<NameScope, Set<KotlinName>>()

    // collects the names that belong to one type
    private val nameTypes = mutableMapOf<NameType, Set<SymbolicName>>()

    private val kotlinNames = mutableMapOf<KotlinName, Set<KotlinName>>()

    private val freshNames = mutableMapOf<FreshName, Set<FreshName>>()

    private val endUpInViper = mutableSetOf<NamedEntity>()

    private val tripleStore = mutableSetOf<Triple<NamedEntity, Relation, NamedEntity>>()
    private val elements = mutableSetOf<NamedEntity>()


    fun addName(scopedName: ScopedKotlinName) {
        elements.add(scopedName)
        tripleStore.add(Triple(scopedName.scope, Relation.SCOPE_OF, scopedName))
        tripleStore.add(Triple(scopedName.name, Relation.SCOPED_NAME, scopedName))
        addName(scopedName.name)
        addName(scopedName.scope)
    }

    fun addName(scope: NameScope) {
        elements.add(scope)
        when (scope) {
            is BadScope -> {}
            is ClassScope -> {
                tripleStore.add(Triple(scope.parent, Relation.SCOPE_OF, scope))
                tripleStore.add(Triple(scope.className, Relation.SCOPED_NAME, scope))
            }

            is FakeScope -> {}
            is LocalScope -> {}
            is PackageScope -> {}
            is ParameterScope -> {}
            is PrivateScope -> {
                tripleStore.add(Triple(scope.parent, Relation.SCOPE_OF, scope))
            }

            is PublicScope -> {
                tripleStore.add(Triple(scope.parent, Relation.SCOPE_OF, scope))
            }
        }
    }


    fun addName(name: NameOfType) {
        elements.add(name)
        when (name) {

            is FunctionTypeName -> {
                tripleStore.add(
                    Triple(name, Relation.DEPENDS_ON, name.args)
                )
                addName(name.args)
                tripleStore.add(
                    Triple(name, Relation.DEPENDS_ON, name.returns)
                )
                addName(name.returns)
            }

            is ListOfNames<*> -> {
                name.names.forEach {
                    tripleStore.add(Triple(name, Relation.DEPENDS_ON, it))
                    addName(it)
                }
            }

            is PretypeName -> {
                // Noting to do
            }


            is TypeName -> {
                tripleStore.add(
                    Triple(name, Relation.DEPENDS_ON, name.pretype.name)
                )
                addName(name.pretype.name)
            }
        }
    }

    fun addName(name: NameType) {
        elements.add(name)
        // nothing to do
    }

    fun addName(name: KotlinName) {
        elements.add(name)
        when (name) {
            is ClassKotlinName -> {
                // nothing to do
            }
            is ConstructorKotlinName -> {
                tripleStore.add(Triple(name, Relation.HAS_TYPE, name.type.name))
                addName(name.type.name)
            }

            is HavocKotlinName -> {
                tripleStore.add(Triple(name, Relation.DEPENDS_ON, name.type.name))
                addName(name.type.name)
            }

            is PredicateKotlinName -> {
                // nothing to do
            }

            is SimpleKotlinName -> {
                // nothing to do
            }

            is TypedKotlinName -> {
                // wil be recorded earlier
            }

            is TypedKotlinNameWithType -> {
                tripleStore.add(Triple(name, Relation.HAS_TYPE, name.type.name))
                addName(name.type.name)
            }
        }

    }

    fun addName(name: FreshName) {
        elements.add(name)
        when (name) {
            is SsaVariableName -> {
                tripleStore.add(Triple(name, Relation.DEPENDS_ON, name.baseName))
                addName(name.baseName)
            }

            else -> {

            }
        }
    }

    fun addName(name: SymbolicName) {
        elements.add(name)
        if (name.nameType != null) {
            tripleStore.add(Triple(name, Relation.NAME_TYPE, name.nameType!!))
            addName(name.nameType!!)
        }

        when (name) {
            is FreshName -> {
                addName(name)
            }

            is KotlinName -> {
                addName(name)
            }

            is ScopedKotlinName -> {
                addName(name)
            }

            is DomainName -> {
                // nothing to do
            }

            is NamedDomainAxiomLabel -> {
                tripleStore.add(Triple(name, Relation.DEPENDS_ON, name.domainName))
            }

            is QualifiedDomainFuncName -> {
                tripleStore.add(Triple(name, Relation.DEPENDS_ON, name.domainName))
            }

            is UnqualifiedDomainFuncName -> {
                // nothing to do
            }

            is NameOfType -> {
                addName(name)
            }
        }
    }

    fun addName(name: NamedEntity) {
        elements.add(name)
        when (name) {
            is NameScope -> addName(name)
            is NameType -> addName(name)
            is SymbolicName -> addName(name)
        }
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
        fun isScoped(obj: NamedEntity): Boolean =
            tripleStore.any { (a, rel, _) -> a == obj && rel == Relation.SCOPED_NAME }

        fun endUpInViper(obj: NamedEntity): Boolean = when (obj) {
            is NameScope -> false // Just scopes should never appear as a actual name in viper
            is NameType -> false // Name Types are only used to distinguish what a name describes
            is SymbolicName -> {
                // maybe
                when (obj) {
                    is FreshName -> true  // likely yes
                    is KotlinName -> !isScoped(obj)
                    is ScopedKotlinName -> true // likely, what if double scoped?
                    is DomainName -> true // yes the domain is used as a name
                    is NamedDomainAxiomLabel -> true
                    is QualifiedDomainFuncName -> true
                    is UnqualifiedDomainFuncName -> true
                    is NameOfType -> false // names of types are not necessary to be unique
                    else -> true
                }
            }

            else -> true
        }

        fun nodeColor(obj: NamedEntity): String {
            if (endUpInViper(obj)) {
                return "lime"
            } else {
                return "black"
            }
        }

        // Helper for labels
        fun label(obj: NamedEntity): String = when (obj) {
            is NameScope -> "Scope: ${obj.name()}"
            is NameType -> "Type: ${obj.name()}"
            is SymbolicName -> {
                when (obj) {
                    is FreshName -> "Fresh: ${obj.name()}"
                    is KotlinName -> "Kotlin: ${obj.name()}"
                    is ScopedKotlinName -> "Scoped: ${obj.name()}"
                    is NameOfType -> "NameOfType: ${obj.name()}"
                    else -> "Domain: ${obj.name()}"
                }
            }
            else -> throw IllegalArgumentException("Unsupported entity type: ${obj.javaClass.name}")
        }


        fun relationLabel(relation: Relation) = when (relation) {
            Relation.SCOPE_OF -> "[color=blue]"
            Relation.DEPENDS_ON -> "[color=black]"
            Relation.HAS_TYPE -> "[color=pink]"
            Relation.SCOPED_NAME -> "[color=lightblue]"
            Relation.NAME_TYPE -> "[color=gray]"
        }

        // add all elements
        elements.forEach {
            sb.appendLine("${id(it)} [label=\"${label(it)}\" color=\"${nodeColor(it)}\"];\n")
        }

        // relations
        tripleStore.forEach { (a, relation, b) ->
            sb.appendLine("${id(a)} -> ${id(b)} ${relationLabel(relation)};\n")
        }


        sb.append("}\n")
        return sb.toString()
    }
}


