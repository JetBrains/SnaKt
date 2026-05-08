package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.*

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

    private fun nextCandidate(name: AnyName): CandidateName {
        val index = currentCandidate.getOrPut(name) { 0 }
        return name.candidates()[index + 1]
    }

    private fun hasNextCandidate(name: AnyName): Boolean {
        val currentIndex = currentCandidate[name] ?: 0
        return currentIndex + 1 < name.candidates().size
    }
    // END RESOLVE NAMES


    // START MANGLE

    /**
     * The cost of having [name] in a viper name.
     * Higher cost means that [name] is more likely to not be used in the final viper name.
     */
    private fun costOfName(name: AnyName): Int = when (name) {
        is NameScope -> when (name) {
            BadScope -> 100
            is ClassScope -> 2
            FakeScope -> 50
            is LocalScope -> 3
            is PackageScope -> 20
            ParameterScope -> 3
            is PrivateScope -> 1
            PublicScope -> 3
        }
        else -> 1
    }

    /**
     * This function calculates the cost of adding [part] to a name.
     * The cost is the sum of the cost of the names that are inside of [part] plus the cost of [part].
     */
    private fun costOfNamePart(part: NamePart): Int = when (part) {
        is NamePart.Basic -> 1
        is NamePart.Dependent -> {
            val name = part.name
            val subParts = currentCandidate(name).parts
            val subCost = subParts.sumOf { costOfNamePart(it) }
            subCost + costOfName(name)
        }

        NamePart.Separator -> 0
    }

    /**
     * The cost of moving [name].
     * The cost of moving is the sum of the costs of the **new** added parts
     */
    private fun costOfMovingName(name: AnyName): Int {
        val currentCandidate = currentCandidate(name)
        val nextCandidate = nextCandidate(name)

        val newParts = (nextCandidate.parts.toSet() - currentCandidate.parts.toSet())
        return newParts.sumOf { costOfNamePart(it) }
    }

    fun removeNonLeaves(toMove: Set<AnyName>): Set<AnyName> {
        val numVisites = mutableMapOf<AnyName, Int>()

        for (name in toMove) {
            val queue = mutableListOf(name)
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                numVisites[current] = (numVisites[current] ?: 0) + 1
                queue.addAll(current.children)
            }
        }

        return toMove.filter { numVisites.getOrDefault(it, 0) < 2 }.toSet()
    }

    /**
     * Generates short human-readable names. Must be called before using [lookup].
     */
    override fun resolve() {
        var currentCollisions = collisions()
        while (currentCollisions.isNotEmpty()) {
            val allToMove = currentCollisions.flatMap { it.value }.mapNotNull { findMoveableName(it)?.second }.toSet()

            val toMove = removeNonLeaves(allToMove)
            assert(toMove.isNotEmpty()) { "No moveable names found, unable to resolve collisions" }

            toMove.forEach(::move)

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
     */
    private fun move(entity: AnyName) {
        currentCandidate[entity] = (currentCandidate[entity] ?: 0) + 1
    }

    /**
     * This function returns the move that results in the lowest cost. If the name cannot be moved, it returns null.
     */
    private fun findMoveableName(entity: AnyName): Pair<Int, AnyName>? {
        val options = buildList {
            val subOptions = currentCandidate(entity).moveableParts().mapNotNull { namePart ->
                findMoveableName(namePart.name)
            }

            addAll(subOptions)

            if (hasNextCandidate(entity)) {
                add(Pair(costOfMovingName(entity), entity))
            }
        }

        return options.minByOrNull { it.first }
    }
}
