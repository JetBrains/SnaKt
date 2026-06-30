package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.formver.type.plugin.TypeUnifier
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

typealias AccessState = PathTrie<Access>

val EmptyAccessState = AccessState(Access.Intermediate)

val AccessState.isTerminal: Boolean
    get() = children.isEmpty() || data == Access.Terminal

/**
 * Performs the join of two [AccessState]s.
 *
 * @param this the [AccessState]
 * @param other the other [AccessState] to join with.
 * @return the [AccessState] containing accesses present in both inputs.
 */
fun AccessState.join(other: AccessState): AccessState =
    join(other, AccessUnifier)

object AccessStateUnifier : TypeUnifier<AccessState> {
    override fun join(left: AccessState, right: AccessState): AccessState =
        left.join(right)
}

/**
 * Returns `true` if the access state refers to a single path, `false` otherwise.
 */
fun AccessState.isSingleton(): Boolean {
    if (children.size > 1) return false

    if (isTerminal && children.isNotEmpty()) return false

    for ((_, child) in children) {
        return child.isSingleton()
    }

    return true
}

/**
 * Concatenates every path in [this] with every path in [other].
 *
 * Worked example (`*` marks `Terminal` nodes; the top-row labels are the trie's own root and are not symbols inside the
 * trie):
 *
 * ```
 *   t0:                paths(t0) = { [b], [b, c], [d] }
 *     \__b*
 *     \  \__c*
 *     \__d*
 *
 *   t1:                paths(t1) = { [f], [g] }
 *     \__f*
 *     \__g*
 *
 *   t0.append(t1):     paths = { [b, f], [b, g], [b, c, f], [b, c, g], [d, f], [d, g] }
 *     \__b
 *     \  \__f*
 *     \  \__g*
 *     \  \__c
 *     \     \__f*
 *     \     \__g*
 *     \__d
 *        \__f*
 *        \__g*
 * ```
 */
fun AccessState.append(other: AccessState): AccessState {
    if (children.isEmpty()) return other

    var newChildren = children

    for ((symbol, child) in children) {
        newChildren = newChildren.put(symbol, child.append(other))
    }

    return if (data == Access.Terminal) {
        for ((symbol, otherChild) in other.children) {
            val thisChild = newChildren[symbol]

            newChildren = newChildren.put(
                symbol,
                thisChild?.join(otherChild) ?: otherChild,
            )
        }
        copy(data = other.data, children = newChildren)
    } else {
        copy(children = newChildren)
    }
}

/**
 * Alters a uniqueness state at every access position specified by this access state.
 *
 * @param context the checker context used for resolving the default uniqueness of the path components.
 * @param this the [AccessState] specifying the access positions to alter.
 * @param uniquenessState the [UniquenessState] to alter.
 * @param transform the function to apply to each access position.
 */
context(context: CheckerContext)
fun AccessState.alterTerminals(
    uniquenessState: UniquenessState,
    transform: (FirBasedSymbol<*>, UniquenessState) -> UniquenessState
): UniquenessState {
    var newUniquenessState = uniquenessState

    for ((symbol, accessChild) in children) {
        val uniquenessChild = uniquenessState.children[symbol]
            ?: UniquenessState(
                symbol.resolveDeclaredUniqueness().join(
                    newUniquenessState.data
                )
            )

        val newUniquenessChild = accessChild
            .alterTerminals(uniquenessChild, transform)

        newUniquenessState = newUniquenessState.associate(
            symbol,
            if (accessChild.isTerminal) {
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
    alterTerminals(uniquenessState) { _, state ->
        if (state.data == Uniqueness.Unique) {
            state.copy(data = Uniqueness.Moved)
        } else {
            state
        }
    }

/**
 * Moves the accessed position specified by this access state in the uniqueness state.
 *
 * @param this the [AccessState] specifying the accessed position to move.
 * @param uniquenessState the [UniquenessState] to move the accessed position in.
 */
context(context: CheckerContext)
fun AccessState.initialize(uniquenessState: UniquenessState): UniquenessState =
    alterTerminals(uniquenessState) { symbol, state ->
        state.copy(data = symbol.resolveDeclaredUniqueness())
    }

/**
 * Enumerates all the paths accessed in [this] access-state
 */
fun AccessState.enumerateTerminals(): Sequence<Path> =
    enumerate { data == Access.Terminal }

/**
 * Project this access state onto the given uniqueness state, creating a uniqueness state containing only the accessed
 * positions specified by this access state.
 *
 * @param context the checker context used for resolving the default uniqueness of the path components.
 * @param this the [AccessState] specifying the accessed positions to project.
 * @param uniquenessState the [UniquenessState] to project onto.
 */
context(context: CheckerContext)
fun AccessState.joinUniquenessOverTerminals(uniquenessState: UniquenessState): Uniqueness {
    var result = Uniqueness.Unique

    for ((symbol, accessChild) in children) {
        val childUniquenessState = uniquenessState.children[symbol]
            ?: UniquenessState(symbol.resolveDeclaredUniqueness())
        val childUniqueness = childUniquenessState.data

        val projected = if (accessChild.data == Access.Terminal) {
            childUniqueness
        } else {
            accessChild.joinUniquenessOverTerminals(childUniquenessState)
        }

        result = result.join(projected)
    }

    return result
}

fun AccessState.joinUniquenessStateOverTerminals(uniquenessState: UniquenessState): UniquenessState {
    var result = EmptyUniquenessState

    for (path in enumerateTerminals()) {
        result = result.join(uniquenessState.find(path) ?: EmptyUniquenessState)
    }

    return result
}
