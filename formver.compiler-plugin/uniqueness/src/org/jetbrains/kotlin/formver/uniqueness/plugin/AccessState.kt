package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.formver.type.plugin.TypeUnifier
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import kotlinx.collections.immutable.persistentMapOf

typealias AccessState = PathTrie<Boolean>

val EmptyAccessState = AccessState(false)

fun AccessState.join(other: AccessState): AccessState =
    join(other) { a, b -> a || b }

object AccessStateUnifier : TypeUnifier<AccessState> {
    override fun join(left: AccessState, right: AccessState): AccessState =
        left.join(right)
}

fun AccessState.isChain(): Boolean {
    if (children.size > 1) return false

    for ((_, child) in children) {
        return child.isChain()
    }

    return true
}

fun AccessState.mask(uniquenessState: UniquenessState, transform: (FirBasedSymbol<*>) -> Uniqueness): UniquenessState {
    var newUniquenessState = uniquenessState

    for ((symbol, accessChild) in children) {
        val uniquenessChild = uniquenessState.children[symbol] ?: EmptyUniquenessState

        val newUniquenessChild = accessChild.mask(uniquenessChild, transform)
            .copy(
                data = if (accessChild.data) transform(symbol) else uniquenessState.data
            )

        newUniquenessState = newUniquenessState.associate(symbol, newUniquenessChild)
    }

    return newUniquenessState
}

fun AccessState.maskMove(uniquenessState: UniquenessState): UniquenessState =
    mask(uniquenessState) { Uniqueness.Moved }

context(context: CheckerContext)
fun AccessState.maskInitialization(uniquenessState: UniquenessState): UniquenessState =
    mask(uniquenessState) { symbol -> symbol.resolveComponentUniqueness() }

context(context: CheckerContext)
fun AccessState.resolveUniqueness(): Uniqueness =
    symbols.fold(Uniqueness.Shared) { result, symbol ->
        result.join(symbol.resolveComponentUniqueness())
    }

private fun UniquenessState.withInheritedUniqueness(inherited: Uniqueness): UniquenessState {
    val current = inherited.join(data)
    var inheritedChildren = persistentMapOf<FirBasedSymbol<*>, UniquenessState>()

    for ((symbol, child) in children) {
        inheritedChildren = inheritedChildren.put(symbol, child.withInheritedUniqueness(current))
    }

    return copy(data = current, children = inheritedChildren)
}

context(context: CheckerContext)
fun AccessState.filter(
    uniquenessState: UniquenessState,
    inheritedUniqueness: Uniqueness = Uniqueness.Unique,
): UniquenessState {
    var filteredState = UniquenessState(Uniqueness.Unique)

    for ((symbol, accessChild) in children) {
        val uniquenessChild = uniquenessState.children[symbol]
            ?: UniquenessState(symbol.resolveComponentUniqueness())

        val filteredChild = if (accessChild.data) {
            uniquenessChild.withInheritedUniqueness(inheritedUniqueness)
        } else {
            accessChild.filter(
                uniquenessChild,
                inheritedUniqueness.join(uniquenessChild.data),
            )
        }

        if (accessChild.data || filteredChild.children.isNotEmpty()) {
            filteredState = filteredState.associate(symbol, filteredChild)
        }
    }

    return filteredState
}
