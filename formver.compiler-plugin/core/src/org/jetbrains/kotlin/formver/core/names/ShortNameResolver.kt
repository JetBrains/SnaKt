package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.SEPARATOR
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.mangled

/**
 * Resolves mangled names into Viper identifiers while maintaining uniqueness.
 * The priority lies on the short and readable names.
 */
class ShortNameResolver : NameResolver {
    // DAG Datastructures

    private val dag: NameDAG<SymbolicName> = NameDAG()


    override fun resolve(name: SymbolicName): String =
        listOfNotNull(name.mangledType, name.mangledScope, name.mangledBaseName).joinToString(SEPARATOR)

    override fun register(name: SymbolicName) {
        name.dependsOn().forEach { dag.addEdge(it, name); register(it) }
        dag.addNode(name)
    }

    fun allUsedNames(): List<String> = dag.keys().map { it.mangled }

    fun duplicateNames(): List<String> = allUsedNames().groupBy { it }.filter { it.value.size > 1 }.keys.toList()

    fun render() = dag.render()
}

class NameDAG<T : SymbolicName> {
    // Maps a parent to its set of children
    private val adjacencyList = mutableMapOf<T, MutableSet<T>>()

    fun addEdge(parent: T, child: T) {
        adjacencyList.getOrPut(parent) { mutableSetOf() }.add(child)
    }

    fun addNode(node: T) {
        if (node in adjacencyList) return
        adjacencyList[node] = mutableSetOf()
    }

    fun keys(): Set<T> = adjacencyList.keys

    fun getChildren(parent: T): Set<T> = adjacencyList[parent] ?: emptySet()

    context(nameResolver: NameResolver)
    fun render(): String {
        val builder = StringBuilder()

        // Start the digraph (directed graph)
        builder.appendLine("digraph NameSystemDAG {")

        // Global styling for a clean look
        builder.appendLine("  node [shape=box, fontname=\"Arial\", style=filled, fillcolor=\"#f9f9f9\"];")
        builder.appendLine("  edge [color=\"#333333\", arrowhead=vee];")

        for (node in adjacencyList.keys) {
            builder.appendLine("  \"${node.mangled}\";")
        }

        builder.appendLine() // Add a break for readability

        // 2. Declare the edges
        for ((parent, children) in adjacencyList) {
            for (child in children) {
                builder.appendLine("  \"${parent.mangled}\" -> \"${child.mangled}\";")
            }
        }

        builder.appendLine("}")
        return builder.toString()
    }
}
