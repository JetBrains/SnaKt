package org.jetbrains.kotlin.formver.uniqueness.plugin

typealias UniquenessTrie = PathTrie<Uniqueness>

val UniquenessRoot = UniquenessTrie(Uniqueness.Global)

fun UniquenessTrie.join(other: UniquenessTrie): UniquenessTrie =
    join(other, UniquenessUnifier)

object UniquenessTrieUnifier {
    fun join(left: UniquenessTrie, right: UniquenessTrie): UniquenessTrie =
        left.join(right)
}

fun UniquenessTrie.meet(other: UniquenessTrie): UniquenessTrie =
    meet(other, UniquenessIntersector)

object UniquenessTrieIntersector {
    fun meet(left: UniquenessTrie, right: UniquenessTrie): UniquenessTrie =
        left.meet(right)
}

fun UniquenessTrie.toUniqueness(): Uniqueness =
    joinChildren(UniquenessUnifier)
