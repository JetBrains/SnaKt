package org.jetbrains.kotlin.formver.uniqueness.plugin

import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.formver.type.plugin.TypeUnifier
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

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

context(context: CheckerContext)
fun AccessState.apply(
    uniquenessState: UniquenessState,
    transform: (FirBasedSymbol<*>, UniquenessState) -> UniquenessState
): UniquenessState {
    var newUniquenessState = uniquenessState

    for ((symbol, accessChild) in children) {
        val uniquenessChild = uniquenessState.children[symbol]
            ?: UniquenessState(symbol.resolveComponentUniqueness())

        val newUniquenessChild = accessChild
            .apply(uniquenessChild, transform)

        newUniquenessState = newUniquenessState.associate(
            symbol,
            if (accessChild.data) {
                transform(symbol, newUniquenessChild)
            } else {
                newUniquenessChild
            }
        )
    }

    return newUniquenessState
}

context(context: CheckerContext)
fun AccessState.move(uniquenessState: UniquenessState): UniquenessState =
    apply(uniquenessState) { _, state -> state.copy(data = Uniqueness.Moved) }

context(context: CheckerContext)
fun AccessState.initialize(uniquenessState: UniquenessState): UniquenessState =
    apply(uniquenessState) { symbol, state -> state.copy(data = symbol.resolveComponentUniqueness()) }

context(context: CheckerContext)
fun AccessState.mask(uniquenessState: UniquenessState): UniquenessState {
    var result = UniquenessState(Uniqueness.Unique)

    for ((symbol, accessChild) in children) {
        val uniquenessChild = uniquenessState.children[symbol]
            ?: UniquenessState(symbol.resolveComponentUniqueness())

        val projected = if (accessChild.data) {
            uniquenessChild
        } else {
            accessChild.mask(uniquenessChild)
        }

        result = result.associate(symbol, projected)
    }

    return result
}

context(context: CheckerContext)
fun AccessState.resolveUniqueness(): Uniqueness =
    symbols.fold(Uniqueness.Unique) { result, symbol ->
        result.join(symbol.resolveComponentUniqueness())
    }
