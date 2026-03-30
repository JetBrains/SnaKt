package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.common.SnaktInternalException
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

    private val currentCandidate = mutableMapOf<NamedEntity, Int>()

    fun resolveFullName(name: NamedEntity): String = when (name) {
        is SymbolicName -> listOfNotNull(
            name.nameType?.fullName(),
            name.mangledScope,
            name.mangledBaseName
        ).joinToString(SEPARATOR)

        is NameScope -> name.mangledScopeName ?: "unknown"
        is NameType -> name.name
        is ViperKeyword -> name.keyword
        else -> "should_never_happen"
    }

    override fun resolve(name: NamedEntity): String = current(name)

    override fun register(name: SymbolicName) {
        graph.addName(name)
    }

    fun render(showCollisions: Boolean = false, showCurrent: Boolean) =
        graph.toGraphviz(this, showCollisions, showCurrent)

    private fun current(name: NamedEntity): String {
        if (name.candidates.isEmpty()) {
            print("empty candidates")
        }
        return currentCandidate(name).name()
    }

    private fun currentCandidate(name: NamedEntity): CandidateName {
        var representative = name
        while (graph.representationMap[representative] != null) {
            representative = graph.representationMap[representative]!!
        }
        return name.candidates[currentCandidate[representative] ?: 0]
    }

    /**
     * Moves `name` one position.
     * We generally try to move first the parts, and only if not successful do we move the `name`
     */
    private fun move(name: NamedEntity): Boolean {
        val movableParts = currentCandidate(name).moveableParts()
        // maybe some priority sorting?
        for (part in movableParts) {
            if (move(part.name)) return true
        }

        if (canMove(name)) {
            currentCandidate[name] = (currentCandidate[name] ?: 0) + 1
            return true
        }
        return false
    }

    private fun canMove(name: NamedEntity): Boolean {
        val currentIndex = currentCandidate[name] ?: 0
        return currentIndex + 1 < name.candidates.size
    }

    fun makeUnique() {
        graph.createRepresentatives()
        var collisions = graph.nameCollisions(this)
        while (collisions.isNotEmpty()) {
            val graphviz = graph.toGraphviz(this, showCollisions = true, showCurrent = true)
            val toResolve = collisions.entries.first().value

            val candidateToMove = toResolve.firstOrNull { canMove(it) }

            if (candidateToMove != null) {
                move(candidateToMove)
            } else {
                throw SnaktInternalException(null, "Unable to make names unique")
            }
            collisions = graph.nameCollisions(this)
        }
    }


}

val CandidateName.schema: String
    get() = parts.joinToString(SEPARATOR) {
        when (it) {
            is NamePart.Basic -> it.name
            is NamePart.Dependent -> when (it.name) {
                is NameScope -> "<scope>"
                is NameType -> "<type>"
                is SymbolicName -> when (it.name) {
                    is FreshName -> "<fresh>"
                    is KotlinName -> "<kotlin>"
                    is ScopedKotlinName -> "<Kscope>"
                    is DomainName -> "<domain>"
                    is NameOfType -> "<typeName>"
                    else -> "<other>"
                }

                else -> it.name.toString()
            }
        }
    }

enum class Relation {
    /** scope SCOPE_OF scopedName **/
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

class ViperKeyword(val keyword: String) : NamedEntity {
    context(nameResolver: NameResolver)
    override fun fullName(): String = keyword

    override val candidates: List<CandidateName> = buildCandidates {
        candidate { +keyword }
    }
}

class NameSystemGraph {

    val viperKeywords = mutableSetOf(
        "import",
        "define",
        "field",
        "method",
        "function",
        "function",
        "predicate",
        "domain",
        "interpretation",
        "returns",
        "unique",
        "requires",
        "ensures",
        "invariant",
        "result",
        "forall",
        "forperm",
        "new",
        "lhs",
        "if",
        "elseif",
        "else",
        "while",
        "fold",
        "unfold",
        "inhale",
        "package",
        "assert",
        "assume",
        "var",
        "label",
        "goto",
        "quasihavoc",
        "quasihavocall",
        "true",
        "false",
        "null",
        "none",
        "wildcard",
        "write",
        "epsilon",
    ).map { ViperKeyword(it) }

    private val tripleStore = mutableSetOf<Triple<NamedEntity, Relation, NamedEntity>>()

    private val elements: MutableSet<NamedEntity> = viperKeywords.toMutableSet()

    val representationMap = java.util.WeakHashMap<NamedEntity, NamedEntity?>()

    var NamedEntity.representedBy: NamedEntity?
        get() = representationMap[this]
        set(value) {
            representationMap[this] = value
        }

    fun dependsOn(elem: NamedEntity): Set<NamedEntity> =
        tripleStore.filter { (a, rel, b) -> a == elem && rel == Relation.DEPENDS_ON }.map { (_, _, b) -> b }.toSet()

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
                addName(scope.parent)
                addName(scope.className)
            }

            is FakeScope -> {}
            is LocalScope -> {}
            is PackageScope -> {}
            is ParameterScope -> {}
            is PrivateScope -> {
                tripleStore.add(Triple(scope.parent, Relation.SCOPE_OF, scope))
                addName(scope.parent)
            }

            is PublicScope -> {
                tripleStore.add(Triple(scope.parent, Relation.SCOPE_OF, scope))
                addName(scope.parent)
            }
        }
    }


    fun addName(name: NameOfType) {
        if (name.nameType != null) {
            tripleStore.add(Triple(name, Relation.NAME_TYPE, name.nameType!!))
            addName(name.nameType!!)
        }
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
        if (name.nameType != null) {
            tripleStore.add(Triple(name, Relation.NAME_TYPE, name.nameType!!))
            addName(name.nameType!!)
        }
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
        if (name.nameType != null) {
            tripleStore.add(Triple(name, Relation.NAME_TYPE, name.nameType!!))
            addName(name.nameType!!)
        }
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
                addName(name.domainName)
            }

            is QualifiedDomainFuncName -> {
                tripleStore.add(Triple(name, Relation.DEPENDS_ON, name.domainName))
                tripleStore.add(Triple(name, Relation.DEPENDS_ON, name.funcName))
                addName(name.domainName)
                addName(name.funcName)
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
                is ScopedKotlinName -> !isScoped(obj) && obj.name !is ClassKotlinName
                is DomainName -> true // yes the domain is used as a name
                is NamedDomainAxiomLabel -> true
                is QualifiedDomainFuncName -> true
                is UnqualifiedDomainFuncName -> false
                is NameOfType -> false // names of types are not necessary to be unique
                else -> true
            }
        }

        is ViperKeyword -> true

        else -> true
    }

    fun createRepresentatives() {
        val publicFields = elements.filter {
            it is ScopedKotlinName && it.scope is PublicScope
        }
        val grouped = mutableMapOf<KotlinName, Set<ScopedKotlinName>>()
        publicFields.forEach {
            grouped.merge((it as ScopedKotlinName).name, setOf(it), Set<ScopedKotlinName>::plus)
        }
        grouped.forEach { (_, fields) ->
            val representative = fields.first()
            fields.drop(1).forEach {
                it.representedBy = representative
            }
        }

    }

    fun nameCollisions(resolver: ShortNameResolver): Map<String, Set<NamedEntity>> {

        // Which names must be considered?
        // - We do need to make all the names unique, but only once that actually end up in viper. E.g a variable and
        //   a scope can have the same name.
        // - Sometimes we want to have name collisions, e.g. public fields with the same name can be merged. This behaviour
        //   is captured with the "representationMap". Hence names that are represented by someone else, must not be considered.
        val toConsider = elements.filter { endUpInViper(it) && it.representedBy == null }
        val names = toConsider.map { Pair(resolver.resolve(it), it) }
        val result = mutableMapOf<String, MutableSet<NamedEntity>>()
        names.forEach { (name, entity) ->
            result.getOrPut(name) { mutableSetOf() }.add(entity)
        }


        return result.filterValues { it.size > 1 }
    }

    fun toGraphviz(resolver: ShortNameResolver, showCollisions: Boolean = false, showCurrent: Boolean = false): String {
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

        fun nodeColor(obj: NamedEntity): String {
            return if (endUpInViper(obj)) {
                "lime"
            } else {
                "black"
            }
        }

        fun candidates(obj: NamedEntity): String = obj.candidates.joinToString("\\n") { it.schema }

        fun nodeLabel(obj: NamedEntity): String {
            var res = ""
            if (showCurrent) res += resolver.resolve(obj) + "\\n---\\n"
            res += candidates(obj)
            return res
        }

        // Helper for labels
        fun label(obj: NamedEntity): String = when (obj) {
            is NameScope -> "Scope: "
            is NameType -> "Type: "
            is SymbolicName -> {
                if (obj.nameType != null) {
                    obj.nameType!!.name + ": "
                } else {
                    when (obj) {
                        is FreshName -> "Fresh: "
                        is KotlinName -> "Kotlin: "
                        is ScopedKotlinName -> "Scoped: "
                        is NameOfType -> "NameOfType: "
                        is DomainName -> "Domain: "
                        is NamedDomainAxiomLabel -> "Axiom: "
                        is QualifiedDomainFuncName -> "Qual Func: "
                        is UnqualifiedDomainFuncName -> "Unqual Func: "
                        else -> "Unknown: "
                    }
                }
            }

            is ViperKeyword -> "Viper Keyword: "
            else -> throw IllegalArgumentException("Unsupported entity type: ${obj.javaClass.name}")
        } + resolver.resolveFullName(obj) + "\\n" + nodeLabel(obj)


        fun relationLabel(relation: Relation) = when (relation) {
            Relation.SCOPE_OF -> "[color=blue]"
            Relation.DEPENDS_ON -> "[color=black]"
            Relation.HAS_TYPE -> "[color=pink]"
            Relation.SCOPED_NAME -> "[color=lightblue]"
            Relation.NAME_TYPE -> "[color=green]"
        }

        fun representLabel() = "[color=purple]"

        // add all elements
        elements.forEach {
            sb.appendLine("\"${id(it)}\" [label=\"${label(it)}\" color=\"${nodeColor(it)}\"];\n")
        }

        if (showCollisions) {
            val collisions = nameCollisions(resolver)
            collisions.keys.forEach {
                sb.appendLine("\"$it\" [label=\"$it\" color=\"red\"];")
            }
            // add name nodes
            collisions.forEach { (key, names) ->
                names.forEach { sb.appendLine("\"${id(it)}\" -> \"$key\" [color=red];") }
            }

        }

        // relations
        tripleStore.forEach { (a, relation, b) ->
            sb.appendLine("\"${id(a)}\" -> \"${id(b)}\" ${relationLabel(relation)};\n")
        }

        representationMap.forEach { (a, b) ->
            if (b != null) {
                sb.appendLine("\"${id(a)}\" -> \"${id(b)}\" ${representLabel()};\n")
            }
        }





        sb.append("}\n")
        return sb.toString()
    }
}


