package org.jetbrains.kotlin.formver.core.names.shortNameResolver

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.names.CandidateName
import org.jetbrains.kotlin.formver.names.NamePart
import org.jetbrains.kotlin.formver.names.candidates
import org.jetbrains.kotlin.formver.viper.*
import org.jetbrains.kotlin.formver.viper.ast.DomainName
import org.jetbrains.kotlin.formver.viper.ast.NamedDomainAxiomLabel
import org.jetbrains.kotlin.formver.viper.ast.QualifiedDomainFuncName
import org.jetbrains.kotlin.formver.viper.ast.UnqualifiedDomainFuncName
import kotlin.math.absoluteValue

/**
 * Gives the current name.
 */
context(resolver: ShortNameResolver)
fun NamePart.name(): String = when (this) {
    is NamePart.Dependent -> name()
    is NamePart.Basic -> this.name
    NamePart.NoSeparator -> ""
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
    NamePart.NoSeparator -> ""
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

fun NamedEntity.fullName(): String = candidates().last().fullName()


// END UTILITY SECTION

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

    REPRESENTED_BY,
}

/**
 * Resolves mangled names into Viper identifiers while maintaining uniqueness.
 * The priority lies on the short and readable names.
 */
class ShortNameResolver : NameResolver {

    /**
     * This Mapping stores the mangled names. It is only initialized after calling `mangle()`
     */
    private lateinit var mangledNames: Map<NamedEntity, String>

    private var _separator = "$"
    val separator
        get() = _separator

    /**
     * Stores which candidate is currently used for a given name.
     * Since the name can depend on other names, recursive lookups may be necessary.
     */
    private val currentCandidate = mutableMapOf<NamedEntity, Int>()

    /**
     * Stores all the names that exist in the system.
     */
    private val elements: MutableSet<NamedEntity> = ViperKeywords.keywords.toMutableSet()

    internal fun addElement(entity: NamedEntity) = elements.add(entity)

    /**
     * All names that appear in Viper. 
     */
    internal fun viperElements() = elements.filter { endUpInViper(it) }

    /**
     * Stores all the relations between names.
     */
    private val tripleStore = mutableSetOf<Triple<NamedEntity, Relation, NamedEntity>>()

    /**
     * Adds a relation between two entities.
     */
    internal fun link(a: NamedEntity, rel: Relation, b: NamedEntity) {
        tripleStore.add(Triple(a, rel, b))
        addElement(a)
        addElement(b)
    }

    /**
     * Collects the user chosen names. They are used to select the deliminator.
     */
    private var longestSequenceDollar = 0
    private var longestSequenceUnderscore = 0
    private fun findLongestAnySequence(input: String, c: Char): Int {
        // Regex matches any character (.) followed by itself \1 zero or more times *
        val pattern = "($c)\\1*".toRegex()

        return pattern.findAll(input).maxOfOrNull { it.value.length } ?: 0
    }

    fun registerUserName(name: String) {
        longestSequenceDollar = maxOf(longestSequenceDollar, findLongestAnySequence(name, '$'))
        longestSequenceUnderscore = maxOf(longestSequenceUnderscore, findLongestAnySequence(name, '_'))
    }

    /**
     * Returns true iff the ``entity`` is scoped. Meaning that there is a name, which wraps around `entity`
     */
    private fun isScoped(entity: NamedEntity): Boolean =
        tripleStore.any { (a, rel, _) -> a == entity && rel == Relation.SCOPED_BY }

    /**
     * Returns the entities `elem` is dependent on.
     */
    fun dependsOn(entity: NamedEntity): Set<NamedEntity> =
        tripleStore.filter { (a, rel, _) -> a == entity && rel == Relation.IS_PART_OF }.map { (_, _, b) -> b }.toSet()

    fun isRepresented(entity: NamedEntity): Boolean = representedBy(entity) != null
    fun representedBy(entity: NamedEntity): NamedEntity? =
        tripleStore.find { (a, rel, _) -> a == entity && rel == Relation.REPRESENTED_BY }?.third


    override fun register(name: NamedEntity) = registerEntity(name)

    private fun registerEntity(entity: NamedEntity) {
        if (elements.contains(entity)) return
        addElement(entity)
        when (entity) {
            is NameScope -> {
                entity.parent?.let {
                    link(it, Relation.SCOPE_OF, entity)
                    registerEntity(it)
                }
                if (entity is ClassScope) {
                    link(entity.className, Relation.SCOPED_BY, entity)
                    registerEntity(entity.className)
                } else if (entity is PackageScope) {
                    registerUserName(entity.packageName.asString())
                }
            }

            is NameType -> {
                // Handled by addElement above
            }

            is SymbolicName -> {
                entity.nameType?.let {
                    link(it, Relation.KIND_OF, entity)
                    registerEntity(it)
                }
                when (entity) {
                    is ScopedName -> {
                        link(entity.name, Relation.SCOPED_BY, entity)
                        registerEntity(entity.name)
                        link(entity.scope, Relation.SCOPE_OF, entity)
                        registerEntity(entity.scope)
                    }

                    is SsaVariableName -> {
                        link(entity.baseName, Relation.IS_PART_OF, entity)
                        registerEntity(entity.baseName)
                    }

                    is ConstructorKotlinName -> {
                        link(entity.type.name, Relation.TYPE_OF, entity)
                        registerEntity(entity.type.name)
                    }

                    is HavocName -> {
                        link(entity.type.name, Relation.IS_PART_OF, entity)
                        registerEntity(entity.type.name)
                    }

                    is TypedKotlinNameWithType -> {
                        link(entity.type.name, Relation.TYPE_OF, entity)
                        registerEntity(entity.type.name)
                    }

                    is DomainName -> {
                        registerUserName(entity.baseName)
                    }

                    is NamedDomainAxiomLabel -> {
                        registerUserName(entity.baseName)
                        link(entity.domainName, Relation.IS_PART_OF, entity)
                        registerEntity(entity.domainName)
                    }

                    is QualifiedDomainFuncName -> {
                        link(entity.domainName, Relation.IS_PART_OF, entity)
                        registerEntity(entity.domainName)
                        link(entity.funcName, Relation.IS_PART_OF, entity)
                        registerEntity(entity.funcName)
                    }

                    is ListOfNames<*> -> {
                        entity.names.forEach {
                            link(it, Relation.IS_PART_OF, entity)
                            registerEntity(it)
                        }
                    }

                    is FunctionTypeName -> {
                        link(entity.args, Relation.IS_PART_OF, entity)
                        registerEntity(entity.args)
                        link(entity.returns, Relation.IS_PART_OF, entity)
                        registerEntity(entity.returns)
                    }

                    is TypeName -> {
                        link(entity.pretype.name, Relation.IS_PART_OF, entity)
                        registerEntity(entity.pretype.name)
                    }

                    is SimpleKotlinName -> registerUserName(entity.name.asString())
                    is ClassKotlinName -> registerUserName(entity.name.asString())
                    is TypedKotlinName -> registerUserName(entity.name.asString())
                    is SpecialFieldName -> registerUserName(entity.name)
                    else -> {}
                }
            }
        }
    }


    // START RESOLVE NAMES

    // The name.fullName() contains characters which are not allowed in viper. This should only happen when we dump the ExpEmbedding, which can contain
    // entities that the final program does not.
    override fun lookup(name: SymbolicName): String = mangledNames.getOrElse(name) {
        name.fullName()
    }

    fun current(entity: NamedEntity): String {
        if (entity.candidates().isEmpty()) {
            print("empty candidates")
        }
        return currentCandidate(entity).name()
    }

    private fun currentCandidate(name: NamedEntity): CandidateName {
        var representative = name
        // find the root of representation
        while (true) {
            representative = representedBy(representative) ?: break
        }
        return name.candidates()[currentCandidate[representative] ?: 0]
    }
    // END RESOLVE NAMES


    // START MANGLE

    /**
     * Generates short human-readable names. Must be called before using [lookup].
     */
    override fun resolve() {
        createRepresentatives()
        chooseSeparator()
        var collisions = collisions()
        while (collisions.isNotEmpty()) {
            val toResolve = collisions.entries.first().value

            val candidateToMove = toResolve.firstOrNull { canMove(it) }

            if (candidateToMove != null) {
                move(candidateToMove)
            } else {
                throw SnaktInternalException(null, "Unable to make names unique")
            }
            collisions = collisions()
        }

        // Fix the names
        mangledNames = viperElements().associateWith { current(it) }
    }

    fun chooseSeparator() {
        _separator = if (longestSequenceDollar < longestSequenceUnderscore) {
            "$".repeat(longestSequenceDollar + 1)
        } else {
            "_".repeat(longestSequenceUnderscore + 1)
        }
    }

    /**
     * For some names (e.g., public fields with the same name), we want to merge them in the viper.
     * If there are multiple such names, and we do nothing, then there will be unnecessary collisions.
     * Hence, this function groups the names into groups that should be merged and choose a representative.
     */
    private fun createRepresentatives() {
        // Field Names which are public
        val publicFields = elements.filter {
            it is ScopedName && it.scope is PublicScope
        }
        // Group them according to their name
        val grouped = mutableMapOf<SymbolicName, Set<ScopedName>>()
        publicFields.forEach {
            grouped.merge((it as ScopedName).name, setOf(it), Set<ScopedName>::plus)
        }
        // The first is the representative.
        // TODO: if one of them already has a representative, this must be taken into account. But can this even happen?
        grouped.forEach { (_, fields) ->
            val representative = fields.first()
            fields.drop(1).forEach {
                link(it, Relation.REPRESENTED_BY, representative)
            }
        }
    }

    /**
     * Returns the name collisions of the current candidates.
     * 
     * Which names must be considered?
     * - We do need to make all the names unique, but only once that actually end up in viper. E.g., a variable and
     *   a scope can have the same name.
     * - Sometimes we want to have name collisions, e.g., public fields with the same name can be merged. This behavior
     *   is captured with the representation system. Hence, names that are represented by someone else must not be considered.
     */
    fun collisions(): Map<String, Set<NamedEntity>> {
        val toConsider = elements.filter { endUpInViper(it) && !isRepresented(it) }
        val names = toConsider.map { Pair(current(it), it) }
        val result = mutableMapOf<String, MutableSet<NamedEntity>>()
        names.forEach { (name, entity) ->
            result.getOrPut(name) {
                mutableSetOf()
            }.add(entity)
        }
        return result.filterValues { it.size > 1 }
    }

    /**
     * Returns true iff the ``entity`` could end up in viper.
     */
    private fun endUpInViper(entity: NamedEntity): Boolean = when (entity) {
        is NameScope -> false // Just scopes should never appear as an actual name in viper
        is NameType -> false // Name Types are only used to distinguish what a name describes
        is SymbolicName -> {
            // maybe
            when (entity) {
                is FreshName -> true  // likely yes
                is KotlinName -> !isScoped(entity)
                is ScopedName -> (!isScoped(entity) && entity.name !is ClassKotlinName)
                is DomainName -> true // yes the domain is used as a name
                is NamedDomainAxiomLabel -> true
                is QualifiedDomainFuncName -> true
                is UnqualifiedDomainFuncName -> false
                is NameOfType -> false
                else -> true
            }
        }

        is ViperKeyword -> true

        else -> false
    }


    /**
     * Moves `name` one position.
     * We generally try to move first the parts, and only if not successful do we move the `name`
     */
    private fun move(entity: NamedEntity): Boolean {
        val movableParts = currentCandidate(entity).moveableParts()
        // maybe some priority sorting?
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
    private fun canMove(entity: NamedEntity): Boolean {
        val currentIndex = currentCandidate[entity] ?: 0
        if (currentIndex + 1 < entity.candidates().size) return true
        return currentCandidate(entity).moveableParts().any {
            canMove(it.name)
        }
    }

    // END MANGLE


    // START RENDER

    fun render(showCollisions: Boolean = true, showCurrent: Boolean = true): String {
        // BEGIN HELPER

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
                        is NameOfType -> "<typeName>"
                        else -> "<other>"
                    }

                    else -> it.name.toString()
                }

                NamePart.NoSeparator -> ""
                NamePart.Separator -> SEPARATOR
            }
        }

        fun candidates(entity: NamedEntity): String = entity.candidates().joinToString(nl) { candidateNameTemplate(it) }

        fun id(entity: NamedEntity): String {
            val hashCode = entity.hashCode() + entity::class.qualifiedName.hashCode()
            return "n${hashCode.absoluteValue}"
        }

        fun nodeColor(entity: NamedEntity): String = if (endUpInViper(entity)) {
            "lime"
        } else {
            "black"
        }


        val sb = StringBuilder()
        sb.appendLine("digraph NameSystem {")
        sb.appendLine("node [shape=box];")


        fun nodeLabel(entity: NamedEntity): String {
            var res = ""
            if (showCurrent) res += current(entity) + "$nl---$nl"
            res += candidates(entity)
            return res
        }

        // Helper for labels
        fun label(entity: NamedEntity): String = when (entity) {
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
            else -> throw SnaktInternalException(null, "Unsupported entity type: ${entity.javaClass.name}")
        } + entity.fullName() + nl + nodeLabel(entity)


        fun relationLabel(relation: Relation) = when (relation) {
            Relation.SCOPE_OF -> "[color=blue]"
            Relation.IS_PART_OF -> "[color=black]"
            Relation.TYPE_OF -> "[color=pink]"
            Relation.SCOPED_BY -> "[color=lightblue]"
            Relation.KIND_OF -> "[color=green]"
            Relation.REPRESENTED_BY -> "[color=purple]"
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

    // END RENDER


}