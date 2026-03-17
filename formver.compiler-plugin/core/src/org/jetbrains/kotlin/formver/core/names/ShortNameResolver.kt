package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SEPARATOR
import org.jetbrains.kotlin.formver.viper.SymbolicName

/**
 * Resolves mangled names into Viper identifiers while maintaining uniqueness.
 * The priority lies on the short and readable names.
 */
class ShortNameResolver : NameResolver {
    // DAG Datastructures

    private val dag: NameDAG<SymbolicName> = NameDAG()

    private val currentCandidates = mutableMapOf<SymbolicName, Iterator<(NameResolver) -> String>>()
    private val currentChosen = mutableMapOf<SymbolicName, (NameResolver) -> String>()

    override fun resolve(name: SymbolicName): String {
        if (name in currentChosen) return currentChosen[name]!!(this)
        val next = currentCandidates.getOrPut(name) { name.candidates.iterator() }.next()
        currentChosen[name] = next
        return next(this)
    }

    fun resolveOld(name: SymbolicName): String =
        listOfNotNull(name.mangledType, name.mangledScope, name.mangledBaseName).joinToString(SEPARATOR)

    private fun updateCandidate(name: SymbolicName) {
        currentCandidates[name]?.let {
            if (it.hasNext()) {
                currentChosen[name] = it.next()
            } else {
                throw SnaktInternalException(null, "Candidate Names exhausted for ${resolveOld(name)}")
            }
        }
    }

    fun resolveConflicts() {
        while (duplicateNames().isNotEmpty()) {
            println("Resolving conflicts for ${duplicateNames().size} names")
            val canUpdate = currentCandidates.filter { it.value.hasNext() }.keys
            val candidate = canUpdate.first()
            println("Updating ${resolveOld(candidate)}")
            updateCandidate(candidate)
        }
    }

    override fun register(name: SymbolicName) {
        name.dependsOn().forEach { dag.addEdge(it, name); register(it) }
        dag.addNode(name, true)
    }

    fun allUsedViperNames(): List<String> = dag.keys().filter { dag.becomesViperName[it] == true }.map { resolve(it) }

    fun duplicateNames(): List<String> = allUsedViperNames().groupBy { it }.filter { it.value.size > 1 }.keys.toList()

    fun render() = dag.render()
}

class NameDAG<T : SymbolicName> {
    // Maps a parent to its set of children
    private val adjacencyList = mutableMapOf<T, MutableSet<T>>()
    val becomesViperName = mutableMapOf<T, Boolean>()

    fun addEdge(parent: T, child: T) {
        adjacencyList.getOrPut(parent) { mutableSetOf() }.add(child)
    }

    fun addNode(node: T, becomesViperName: Boolean) {
        adjacencyList.getOrPut(node) { mutableSetOf() }
        this.becomesViperName[node] = this.becomesViperName.getOrDefault(node, false) || becomesViperName
    }

    fun keys(): Set<T> = adjacencyList.keys

    fun getChildren(parent: T): Set<T> = adjacencyList[parent] ?: emptySet()

    context(nameResolver: ShortNameResolver)
    fun render(): String {
        val builder = StringBuilder()

        // Start the digraph (directed graph)
        builder.appendLine("digraph NameSystemDAG {")

        // Global styling for a clean look
        builder.appendLine("  node [shape=box, fontname=\"Arial\", style=filled, fillcolor=\"#f9f9f9\"];")
        builder.appendLine("  edge [color=\"#333333\", arrowhead=vee];")

        for (node in adjacencyList.keys) {
            builder.appendLine("  \"${nameResolver.resolveOld(node)}\" [label=\"${nameResolver.resolve(node)} ${becomesViperName[node]}\"];")
        }

        builder.appendLine() // Add a break for readability

        // 2. Declare the edges
        for ((parent, children) in adjacencyList) {
            for (child in children) {
                builder.appendLine("  \"${nameResolver.resolveOld(parent)}\" -> \"${nameResolver.resolveOld(child)}\";")
            }
        }

        builder.appendLine("}")
        return builder.toString()
    }
}
