package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.formver.type.plugin.TypeUnifier
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

typealias AccessState = PathTrie<Boolean>

val EmptyAccessState = AccessState(false)

/**
 * Performs the join of two [AccessState]s.
 *
 * @param this the [AccessState]
 * @param other the other [AccessState] to join with.
 * @return the [AccessState] containing accesses present in both inputs.
 */
fun AccessState.join(other: AccessState): AccessState =
    join(other) { a, b -> a || b }

object AccessStateUnifier : TypeUnifier<AccessState> {
    override fun join(left: AccessState, right: AccessState): AccessState =
        left.join(right)
}

/**
 * Returns `true` if the access state refers to a single path, `false` otherwise.
 */
fun AccessState.isSingleton(): Boolean {
    if (children.size > 1) return false

    for ((_, child) in children) {
        return child.isSingleton()
    }

    return true
}

/**
 * Alters a uniqueness state at every access position specified by this access state.
 *
 * @param this the [AccessState] specifying the access positions to alter.
 * @param uniquenessState the [UniquenessState] to alter.
 * @param transform the function to apply to each access position.
 */
context(context: CheckerContext)
fun AccessState.alter(
    uniquenessState: UniquenessState,
    transform: (FirBasedSymbol<*>, UniquenessState) -> UniquenessState
): UniquenessState {
    var newUniquenessState = uniquenessState

    for ((symbol, accessChild) in children) {
        val uniquenessChild = uniquenessState.children[symbol]
            ?: UniquenessState(symbol.resolveComponentUniqueness())

        val newUniquenessChild = accessChild
            .alter(uniquenessChild, transform)

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

/**
 * Moves the accessed position specified by this access state in the uniqueness state.
 *
 * @param this the [AccessState] specifying the accessed position to move.
 * @param uniquenessState the [UniquenessState] to move the accessed position in.
 */
context(context: CheckerContext)
fun AccessState.move(uniquenessState: UniquenessState): UniquenessState =
    alter(uniquenessState) { _, state -> state.copy(data = Uniqueness.Moved) }

/**
 * Moves the accessed position specified by this access state in the uniqueness state.
 *
 * @param this the [AccessState] specifying the accessed position to move.
 * @param uniquenessState the [UniquenessState] to move the accessed position in.
 */
context(context: CheckerContext)
fun AccessState.initialize(uniquenessState: UniquenessState): UniquenessState =
    alter(uniquenessState) { symbol, state -> state.copy(data = symbol.resolveComponentUniqueness()) }

/**
 * Project this access state onto the given uniqueness state, creating a uniqueness state containing only the accessed
 * positions specified by this access state.
 *
 * @param this the [AccessState] specifying the accessed positions to project.
 * @param uniquenessState the [UniquenessState] to project onto.
 */
context(context: CheckerContext)
fun AccessState.project(uniquenessState: UniquenessState): UniquenessState {
    var result = UniquenessState(Uniqueness.Unique)

    for ((symbol, accessChild) in children) {
        val uniquenessChild = uniquenessState.children[symbol]
            ?: UniquenessState(symbol.resolveComponentUniqueness())

        val projected = if (accessChild.data) {
            uniquenessChild
        } else {
            accessChild.project(uniquenessChild)
        }

        result = result.associate(symbol, projected)
    }

    return result
}
