package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.formver.type.plugin.TypeIntersector
import org.jetbrains.kotlin.formver.type.plugin.TypeUnifier

typealias UniquenessState = PathTrie<Uniqueness>

val EmptyUniquenessState = UniquenessState(Uniqueness.Unique)

fun UniquenessState.join(other: UniquenessState): UniquenessState =
    join(other, UniquenessUnifier)

object UniquenessStateUnifier : TypeUnifier<UniquenessState> {
    override fun join(left: UniquenessState, right: UniquenessState): UniquenessState =
        left.join(right)
}

fun UniquenessState.meet(other: UniquenessState): UniquenessState =
    meet(other, UniquenessIntersector)

object UniquenessStateIntersector : TypeIntersector<UniquenessState> {
    override fun meet(left: UniquenessState, right: UniquenessState): UniquenessState =
        left.meet(right)
}

fun UniquenessState.joinOverPath(path: List<FirBasedSymbol<*>>): Uniqueness =
    data.join((children[path.first()]?.joinOverPath(path.drop(1)) ?: Uniqueness.Unique))

fun UniquenessState.enumerateMoved(): Sequence<List<FirBasedSymbol<*>>> =
    enumerate(emptyList()) { data == Uniqueness.Moved }
