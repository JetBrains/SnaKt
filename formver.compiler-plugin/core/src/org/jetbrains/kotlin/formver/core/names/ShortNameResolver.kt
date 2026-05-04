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
    // END RESOLVE NAMES


    // START MANGLE


    private fun costOfName(name: AnyName) : Int = when(name) {
        is NameScope -> when(name) {
            BadScope -> 100
            is ClassScope -> 2
            FakeScope -> 50
            is LocalScope -> 3
            is PackageScope -> 10
            ParameterScope -> 3
            is PrivateScope -> 1
            PublicScope -> 3
        }
        else -> 1
    }

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
     * TODO: Update
     * The higher the priority, the earlier it is chosen to move.
     * There are no fundamental reasons for this priority order. These just resulted in "good" names.
     */
    private fun costOfMovingName(name: AnyName): Int {
        val currentCandidate = currentCandidate(name)
        val nextCandidate = nextCandidate(name)

        val newParts = (nextCandidate.parts.toSet() - currentCandidate.parts.toSet())
        return newParts.sumOf { costOfNamePart(it)}
    }

    private fun buildGraph(name: AnyName): Map<AnyName, Set<AnyName>> {
        val graph = mutableMapOf<AnyName, MutableSet<AnyName>>()
        val queue = mutableListOf(name)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val subNames = current.children
            subNames.forEach {
                graph.getOrPut(it) { mutableSetOf() }.add(current)
            }
            queue.addAll(subNames)
        }
        return graph
    }

    private fun buildGraph(names: Set<AnyName>) : Map<AnyName, Set<AnyName>> {
        return names.fold(emptyMap()
        ) {
            acc, name -> acc +  buildGraph(name)
        }
    }

    /**
     * Generates short human-readable names. Must be called before using [lookup].
     */
    override fun resolve() {
        var currentCollisions = collisions()
        loop@ while (currentCollisions.isNotEmpty()) {
            val toMove = currentCollisions.flatMap { it.value }.mapNotNull { findMoveableName(it)?.second }.toSet()
            // outer name maps to inner name
            val graph = buildGraph(toMove)
            assert(toMove.isNotEmpty()) { "No moveable names found, unable to resolve collisions" }

            toMove.forEach { name ->
                if (graph[name]?.isNotEmpty() != true) {
//                    print(current(name))
                    move(name)
//                    println(" -> ${current(name)}")
                }
            }



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
     * Returns the name that will be moved. This can be the [entity] or a name on which [entity] depends.
     * If no such name exists, it returns null.
     */
    private fun findMoveableName(entity: AnyName): Pair<Int, AnyName>? {
        val currentIndex = currentCandidate[entity] ?: 0
        val options = currentCandidate(entity).moveableParts().mapNotNull { namePart ->
            findMoveableName(namePart.name)
        } + (if (currentIndex + 1 < entity.candidates().size)  {
            // entity can be moved, so it is added to the options
            listOf(Pair(costOfMovingName(entity), entity))
        } else emptyList())
        return options.minByOrNull { it.first }
    }
}
