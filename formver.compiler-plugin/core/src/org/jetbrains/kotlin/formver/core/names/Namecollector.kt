package org.jetbrains.kotlin.formver.core.names

object CreatedNames {
    private var names: MutableSet<OurName> = mutableSetOf()

    var dag: MutableMap<OurName, Set<OurName>> = mutableMapOf()
    var resolver = Resolver()

    fun add(name: OurName) {
        names.add(name)
        val parents = name.parents()
        parents.forEach {
            dag.getOrPut(it) { mutableSetOf<OurName>() } + name
        }
    }

    fun toGraphviz(): String {
        val sb = StringBuilder()
        sb.appendLine("digraph G {")
        for (name in names) {
            val id = name.hashCode()
            sb.appendLine("  $id [label=\"${name.shortName()}\"]")
            for (parent in name.parents()) {
                val parentId = parent.hashCode()
                sb.appendLine("  $parentId -> $id")
            }
        }
        sb.appendLine("}")
        return sb.toString()
    }

    fun allCollisions(): List<OurName> {
        return names.groupBy { resolver.currentCandidate(it) }.filter { it.value.size > 1 }.values.flatten()
    }

    fun listAllCollisions(): String {
        val builder = StringBuilder()
        val collisions = allCollisions()
        builder.appendLine("Collision")
        collisions.forEach { builder.appendLine(it.shortName()) }
        return builder.toString()
    }

    fun selectToResolve(): OurName? {
        val collisions = allCollisions()
        if (collisions.isEmpty()) return null

        val canMove = collisions.first { resolver.hasNext(it) }

        return canMove
    }

    fun makeCollisionFree() {
        var change = selectToResolve()
        while (change != null) {
            resolver.move(change)
            change = selectToResolve()
        }
    }

    fun getMapping(): String = names.joinToString("\n") {
        val key = it.longName()
        val name = resolver.currentCandidate(it)
        "$key -> $name"
    }

}


class Resolver() {
    val candidate: MutableMap<OurName, Int> = mutableMapOf()

    fun currentCandidate(x: OurName): String {
        return x.candidate(candidate.getOrPut(x) { 0 }, this)
    }

    fun hasNext(x: OurName): Boolean {
        return candidate.getOrPut(x) { 0 } < x.numCandidates - 1
    }

    fun move(x: OurName) {
        candidate[x] = candidate.getOrPut(x) { 0 } + 1
    }
}