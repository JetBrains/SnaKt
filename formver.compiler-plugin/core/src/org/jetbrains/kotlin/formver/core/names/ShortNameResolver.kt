package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.viper.*
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import kotlin.math.absoluteValue

/**
 * Gives the current name.
 */
context(resolver: ShortNameResolver)
fun NamePart.name(): String = when (this) {
    is NamePart.Dependent -> name()
    is NamePart.Basic -> this.name
    NamePart.Separator -> resolver.separator
}

/**
 * Gives the current name.
 */
context(resolver: ShortNameResolver)
fun NamePart.Dependent.name(): String = resolver.current(name)

/**
 * Gives the current name.
 */
context(resolver: ShortNameResolver)
fun CandidateName.name(): String = parts.joinToString("") { it.name() }


/**
 * Gives the most specific name.
 */
fun NamePart.fullName(): String = when (this) {
    is NamePart.Dependent -> fullName()
    is NamePart.Basic -> this.name
    NamePart.Separator -> SEPARATOR
}

/**
 * Gives the most specific name.
 */
fun NamePart.Dependent.fullName(): String = name.candidates().last().fullName()

/**
 * Gives the most specific name.
 */
fun CandidateName.fullName(): String = if (parts.size > 1) {
    "[" + parts.joinToString("") { it.fullName() } + "]"
} else {
    parts.joinToString("") { it.fullName() }
}

/**
 * Gives the most specific name.
 */
fun AnyName.fullName(): String = candidates().last().fullName()


// END UTILITY SECTION

/**
 * Resolves mangled names into Viper identifiers while maintaining uniqueness.
 * The priority lies on the short and readable names.
 */
class ShortNameResolver : NameResolver {

    /**
     * This Mapping stores the mangled names. It is only initialized after calling [resolve]
     */
    private lateinit var mangledNames: Map<AnyName, String>

    val separator = "$"

    /**
     * Stores which candidate is currently used for a given name.
     * Since the name can depend on other names, recursive lookups may be necessary.
     */
    private val currentCandidate = mutableMapOf<AnyName, Int>()

    /**
     * Stores all the names that exist in the system.
     */
    private val elements: MutableSet<AnyName> = ViperKeywords.keywords.toMutableSet()

    private fun addElement(entity: AnyName) = elements.add(entity)

    /**
     * All names that appear in Viper. 
     */
    private fun viperElements() = elements.filter { it.inViper }


    override fun register(name: AnyName) {
        if (elements.contains(name)) return
        addElement(name)
        name.children.forEach {
            register(it)
        }
    }

    // START RESOLVE NAMES


    override fun lookup(name: SymbolicName): String = mangledNames.getOrElse(name) {
        // The name.fullName() contains characters which are not allowed in viper. This should only happen when we dump the ExpEmbedding, which can contain
        // entities that the final program does not.
        name.fullName()
    }

    internal fun current(entity: AnyName): String {
        return currentCandidate(entity).name()
    }

    private fun currentCandidate(name: AnyName): CandidateName {
        val index = currentCandidate.getOrPut(name) { 0 }
        return name.candidates()[index]
    }
    // END RESOLVE NAMES


    // START MANGLE

    /**
     * The higher the priority, the earlier it is chosen to move.
     * There are no fundamental reasons for this priority order. These just resulted in "good" names.
     */
    private fun priorityOrder(name: AnyName): Int = when (name) {
        is SymbolicName -> when (name) {
            is FreshName -> when (name) {
                is LabelName -> 5
                else -> 1
            }
            is KotlinName -> 1
            is ScopedName -> 2
            is DomainName -> 4
            else -> -1
        }
        else -> -1
    }


    /**
     * Generates short human-readable names. Must be called before using [lookup].
     */
    override fun resolve() {
        var currentCollisions = collisions()
        while (currentCollisions.isNotEmpty()) {
            val toResolve = currentCollisions.entries.first().value

            val candidateToMove = toResolve.filter { canMove(it) }.ifNotEmpty { maxBy { priorityOrder(it) } }

            assert(candidateToMove != null) { "Unable to make names unique" }

            move(candidateToMove!!)
            currentCollisions = collisions()
        }

        // Fix the names
        mangledNames = viperElements().associateWith { current(it) }
    }

    /**
     * Returns the name collisions of the current candidates.
     * 
     * Which names must be considered?
     * - We do need to make all the names unique, but only once that actually end up in viper. E.g., a variable and
     *   a scope can have the same name.
     */
    private fun collisions(): Map<String, Set<AnyName>> {
        val names = viperElements().map { Pair(current(it), it) }
        val result = mutableMapOf<String, MutableSet<AnyName>>()
        names.forEach { (name, entity) ->
            result.getOrPut(name) {
                mutableSetOf()
            }.add(entity)
        }
        return result.filterValues { it.size > 1 }
    }

    /**
     * Moves `name` one position.
     * We generally try to move first the parts, and only if not successful do we move the `name`
     *
     * Returns true if the entity (or a dependent name) could be moved
     */
    private fun move(entity: AnyName): Boolean {
        val movableParts = currentCandidate(entity).moveableParts()

        // Try to move the subparts first
        for (part in movableParts) {
            if (move(part.name)) return true
        }

        if (canMove(entity)) {
            currentCandidate[entity] = (currentCandidate[entity] ?: 0) + 1
            return true
        }
        return false
    }
    /**
     * Returns true if `name` can be moved or one of the parts of the current candidate can be moved
     */
    private fun canMove(entity: AnyName): Boolean {
        val currentIndex = currentCandidate[entity] ?: 0
        if (currentIndex + 1 < entity.candidates().size) return true
        return currentCandidate(entity).moveableParts().any {
            canMove(it.name)
        }
    }

    // END MANGLE


    // START RENDER


    internal enum class Relation {

        /** scope SCOPE_OF scopedName **/
        SCOPE_OF,

        /** name SCOPED_BY nameScoped **/
        SCOPED_BY,

        /** Int TYPE_OF Function**/
        TYPE_OF,

        /** a IS_PART_OF (a,b) -> c **/
        IS_PART_OF,

        /** name NAME_TYPE nameType **/
        KIND_OF,
    }

    /**
     * Stores all the relations between names.
     */
    private val tripleStore = mutableSetOf<Triple<AnyName, Relation, AnyName>>()

    /**
     * Adds a relation between two entities.
     */
    private fun link(a: AnyName, rel: Relation, b: AnyName) {
        tripleStore.add(Triple(a, rel, b))

    }

    /**
     * This method generates a dot graph of the current state of the short name resolver.
     *
     * The graph shows the current candidate for each name, and the dependencies between them.
     *
     * If `showCollisions` is true, the graph will show which names collide with each other.
     *
     * If `showCurrent` is true, the graph will also show the current candidate for each name.
     */
    fun render(showCollisions: Boolean = true, showCurrent: Boolean = true): String {

        if (tripleStore.isEmpty()) registerRelation()

        val nl = "\\n"

        /**
         * Returns the candidate Name, but not recursively resolved. Recursive Names are replaced with
         * placeholders.
         */
        fun candidateNameTemplate(candidate: CandidateName): String = candidate.parts.joinToString() {
            when (it) {
                is NamePart.Basic -> it.name
                is NamePart.Dependent -> when (it.name) {
                    is NameScope -> "<scope>"
                    is NameType -> "<type>"
                    is SymbolicName -> when (it.name) {
                        is FreshName -> "<fresh>"
                        is KotlinName -> "<kotlin>"
                        is ScopedName -> "<Kscope>"
                        is DomainName -> "<domain>"
                        is PretypeName -> "<typeName>"
                        else -> "<other>"
                    }

                    else -> it.name.toString()
                }
                NamePart.Separator -> SEPARATOR
            }
        }

        fun candidates(entity: AnyName): String = entity.candidates().joinToString(nl) { candidateNameTemplate(it) }

        fun id(entity: AnyName): String {
            val hashCode = entity.hashCode() + entity::class.qualifiedName.hashCode()
            return "n${hashCode.absoluteValue}"
        }

        fun nodeColor(entity: AnyName): String = if (entity.inViper) {
            "lime"
        } else {
            "black"
        }


        val sb = StringBuilder()
        sb.appendLine("digraph NameSystem {")
        sb.appendLine("node [shape=box];")


        fun nodeLabel(entity: AnyName): String {
            var res = ""
            if (showCurrent) res += current(entity) + "$nl---$nl"
            res += candidates(entity)
            return res
        }

        // Helper for labels
        fun label(entity: AnyName): String = when (entity) {
            is NameScope -> "Scope: "
            is NameType -> "Type: "
            is SymbolicName -> {
                if (entity.nameType != null) {
                    entity.nameType!!.fullName() + ": "
                } else {
                    when (entity) {
                        is FreshName -> "Fresh: "
                        is KotlinName -> "Kotlin: "
                        is ScopedName -> "Scoped: "
                        is PretypeName -> "PretypeName: "
                        is DomainName -> "Domain: "
                        is NamedDomainAxiomLabel -> "Axiom: "
                        is QualifiedDomainFuncName -> "Qual Func: "
                        is UnqualifiedDomainFuncName -> "Unqual Func: "
                        else -> "Unknown: "
                    }
                }
            }

            is ViperKeyword -> "Viper Keyword: "
            else -> throw SnaktInternalException(null, "Unsupported entity type: ${entity.javaClass.name}")
        } + entity.fullName() + nl + nodeLabel(entity)


        fun relationLabel(relation: Relation) = when (relation) {
            Relation.SCOPE_OF -> "[color=blue]"
            Relation.IS_PART_OF -> "[color=black]"
            Relation.TYPE_OF -> "[color=pink]"
            Relation.SCOPED_BY -> "[color=lightblue]"
            Relation.KIND_OF -> "[color=green]"
        }


        // add all elements
        elements.forEach {
            sb.appendLine("\"${id(it)}\" [label=\"${label(it)}\" color=\"${nodeColor(it)}\"];\n")
        }

        if (showCollisions) {
            val collisions = collisions()
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

        sb.append("}\n")
        return sb.toString()
    }

    private fun registerRelation() {
        elements.forEach { registerRelationEntity(it) }
    }

    private fun registerRelationFreshName(entity: FreshName) {
        when (entity) {
            is SsaVariableName -> {
                link(entity.baseName, Relation.IS_PART_OF, entity)
            }

            is HavocName -> {
                link(entity.type.name, Relation.IS_PART_OF, entity)
            }
            // No else branch, because FreshName is sealed. When adding a new name, the compiler will complain here.
            FunctionResultVariableName, PlaceholderReturnVariableName, ExtensionReceiverName, DispatchReceiverName,
            is AnonymousBuiltinName, is AnonymousName, is PlaceholderArgumentName,
            is ReturnVariableName, is BreakLabelName, is CatchLabelName,
            is ContinueLabelName, is TryExitLabelName, is ReturnLabelName,
            is DomainAssociatedFuncName, is DomainFuncParameterName, is PredicateName, is SpecialFieldName -> {
            }
        }
    }

    private fun registerRelationEntity(entity: AnyName) {
        addElement(entity)
        when (entity) {
            is NameScope -> {
                entity.parent?.let {
                    link(it, Relation.SCOPE_OF, entity)
                }
                if (entity is ClassScope) {
                    link(entity.className, Relation.SCOPED_BY, entity)
                }
            }

            // Handled by addElement above
            is NameType -> {}

            is SymbolicName -> {
                entity.nameType?.let {
                    link(it, Relation.KIND_OF, entity)
                }
                when (entity) {
                    is FreshName -> registerRelationFreshName(entity)
                    is ScopedName -> {
                        link(entity.name, Relation.SCOPED_BY, entity)
                        link(entity.scope, Relation.SCOPE_OF, entity)
                    }

                    is ConstructorKotlinName -> {
                        link(entity.type.name, Relation.TYPE_OF, entity)
                    }

                    is TypedKotlinNameWithType -> {
                        link(entity.type.name, Relation.TYPE_OF, entity)
                    }

                    is NamedDomainAxiomLabel -> {
                        link(entity.domainName, Relation.IS_PART_OF, entity)
                    }

                    is QualifiedDomainFuncName -> {
                        link(entity.domainName, Relation.IS_PART_OF, entity)
                        link(entity.funcName, Relation.IS_PART_OF, entity)
                    }

                    is ListOfNames<*> -> {
                        entity.names.forEach {
                            link(it, Relation.IS_PART_OF, entity)
                        }
                    }

                    is FunctionTypeName -> {
                        link(entity.args, Relation.IS_PART_OF, entity)
                        link(entity.returns, Relation.IS_PART_OF, entity)
                    }

                    is TypeName -> {
                        link(entity.pretype.name, Relation.IS_PART_OF, entity)
                    }

                    is SimpleKotlinName, is ClassKotlinName, is TypedKotlinName, is DomainName, is UnqualifiedDomainFuncName, is PretypeName -> {}
                    else -> {
                        throw SnaktInternalException(null, "Unsupported entity type: ${entity.javaClass.name}")
                    }
                }
            }
        }
    }

    // END RENDER


}
