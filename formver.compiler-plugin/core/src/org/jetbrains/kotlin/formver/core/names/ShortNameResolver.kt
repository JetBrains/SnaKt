package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.*
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

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

    val separator = "_"

    /**
     * Stores which candidate is currently used for a given name.
     * Since the name can depend on other names, recursive lookups may be necessary.
     */
    private val currentCandidate = mutableMapOf<AnyName, Int>()

    /**
     * Stores all the names that exist in the system.
     */
    private val elements: MutableSet<AnyName> = ViperKeywords.keywords.toMutableSet()

    fun elements() = elements.toList()

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
    internal fun collisions(): Map<String, Set<AnyName>> {
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
}
