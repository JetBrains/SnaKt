package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

typealias AccessState = PathTrie<Access>

val EmptyAccessState = AccessState(Access.Intermediate)

/**
 * Returns `true` if [this] [AccessState] represents the end of a path (terminal), `false` otherwise.
 */
val AccessState.isTerminal: Boolean
    get() = children.isEmpty() || data == Access.Terminal

/**
 * Enumerates all the paths accessed in [this] access-state.
 */
fun AccessState.enumeratePaths(): Sequence<Path> =
    enumerate { data == Access.Terminal }

/**
 * Performs the join of two [AccessState]s.
 *
 * @param this the [AccessState]
 * @param other the other [AccessState] to join with.
 * @return the [AccessState] containing accesses present in both inputs.
 */
fun AccessState.join(other: AccessState): AccessState =
    join(other, Access::join)

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
 *
 * In this example, every path in `t1` is appended after every terminal path in `t0`. As a result, a node that is
 * [Terminal] in `t0` can become [Intermediate] in `t0.append(t1)`, because it is no longer an endpoint and now has
 * children contributed by `t1`.
 * ```
 */
fun AccessState.append(other: AccessState): AccessState {
    if (children.isEmpty()) return other

    var newChildren = children

    for ((symbol, child) in children) {
        newChildren = newChildren.put(symbol, child.append(other))
    }

    if (data == Access.Terminal) {
        for ((symbol, otherChild) in other.children) {
            val thisChild = newChildren[symbol]

            newChildren = newChildren.put(
                symbol,
                thisChild?.join(otherChild) ?: otherChild,
            )
        }

        return copy(data = other.data, children = newChildren)
    } else {
        return copy(children = newChildren)
    }
}

/**
 * Alters a uniqueness state at every access position specified by this access state.
 *
 * @param context the checker context used for resolving the default uniqueness of the path components.
 * @param this the [AccessState] specifying the access positions to alter.
 * @param uniquenessState the [UniquenessState] to alter.
 * @param transform the function to apply to each access position.
 *
 * If any of the intermediate path components is not resolved within [uniquenessState], the uniqueness of those
 * components is automatically inferred to be the join between the declared uniqueness of the component's symbol and the
 * uniqueness of the parent.
 *
 * An access that reaches a summary node cannot single out one path in the region the summary covers, so the update
 * there is weak: the transform is applied to the value the accessed path would have if it were not recorded, and the
 * result is joined into the summary. The summary's own value cannot stand in for the accessed path, because it is only
 * an upper bound: a summary that is shared because of one path may still cover a unique path that a move has to mark.
 */
context(context: CheckerContext)
fun AccessState.transformOnTerminals(
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

        val newUniquenessChild = if (uniquenessChild.summarizesDescendants) {
            val ownPath = listOf(symbol)
            val paths = accessChild.enumeratePaths().map { ownPath + it }
            val accessedPaths = if (accessChild.isTerminal) sequenceOf(ownPath) + paths else paths

            accessedPaths.fold(uniquenessChild) { summary, path ->
                val unrecorded = UniquenessState(path.resolveDeclaredUniqueness().join(newUniquenessState.data))
                summary.copy(data = summary.data.join(transform(path.last(), unrecorded).data))
            }
        } else {
            val transformedChild = accessChild.transformOnTerminals(uniquenessChild, transform)

            if (accessChild.isTerminal) transform(symbol, transformedChild) else transformedChild
        }

        newUniquenessState = newUniquenessState.putChild(symbol, newUniquenessChild)
    }

    return newUniquenessState
}

/**
 * Restores the accessed position specified by this access state in the uniqueness state to its declared uniqueness.
 *
 * @param this the [AccessState] specifying the accessed position to initialize.
 * @param uniquenessState the [UniquenessState] to initialize the accessed position in.
 */
context(context: CheckerContext)
fun AccessState.initialize(uniquenessState: UniquenessState): UniquenessState =
    transformOnTerminals(uniquenessState) { symbol, state ->
        state.copy(data = symbol.resolveDeclaredUniqueness())
    }

/**
 * Moves the accessed position specified by this access state in the uniqueness state.
 *
 * @param this the [AccessState] specifying the accessed position to move.
 * @param uniquenessState the [UniquenessState] to move the accessed position in.
 */
context(context: CheckerContext)
fun AccessState.move(uniquenessState: UniquenessState): UniquenessState =
    transformOnTerminals(uniquenessState) { _, state ->
        if (state.data <= Uniqueness.Unknown) {
            state.copy(data = Uniqueness.Moved)
        } else {
            state
        }
    }

/**
 * Joins the uniqueness values at the terminal access paths of this access state.
 *
 * @param this the [AccessState] specifying the terminal paths to read.
 * @param uniquenessState the [UniquenessState] to read terminal uniqueness values from.
 * @return the join of all terminal uniqueness values, or [Uniqueness.Unique] when no terminal path is present.
 */
fun AccessState.projectTerminalUniqueness(uniquenessState: UniquenessState): Uniqueness {
    var result = Uniqueness.Unique

    for (path in enumeratePaths()) {
        result = result.join(uniquenessState.find(path)?.data ?: Uniqueness.Unique)
    }

    return result
}

/**
 * Projects the uniqueness substates at the terminal access paths of this access state.
 *
 * @param this the [AccessState] specifying the terminal paths to project.
 * @param uniquenessState the [UniquenessState] to project terminal substates from.
 * @return a joined [UniquenessState] containing the substates found at all terminal paths.
 */
fun AccessState.projectTerminalUniquenessState(uniquenessState: UniquenessState): UniquenessState {
    var result = EmptyUniquenessState

    for (path in enumeratePaths()) {
        result = result.join(uniquenessState.find(path) ?: EmptyUniquenessState)
    }

    return result
}
