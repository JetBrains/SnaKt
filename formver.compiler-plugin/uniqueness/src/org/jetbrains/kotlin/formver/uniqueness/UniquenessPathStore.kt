package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

/**
 * Stores unique context for each path in a trie structure.
 *
 * Each path represents a sequence of symbols like x.y.z, where each node has a uniqueness type annotation
 *
 * Example:
 * ```
 *     x.y.w => local/x --> A/y -> B/w -> ..
 *                 ^     |
 *     x.z.w ======+     +-> A/z -> C/w -> ...
 * ```
 */
interface UniquenessPathStore {
    var type: UniquenessType

    /**
     * Retrieves the child node corresponding to the given path within the trie structure,
     * or creates and inserts a new path if it does not already exist.
     * This method uses the provided context to resolve unique annotations for new nodes.
     *
     * @param path A list of [FirBasedSymbol] items representing the path to traverse or create in the trie.
     *             Each symbol corresponds to a hierarchical level in the path.
     * @return The [UniquenessPathStore] node at the end of the given path, creating intermediate nodes as necessary.
     */
    fun ensurePath(path: Path): UniquenessPathStore

    /**
     * Represents the least upper bound (LUB) of uniqueness levels for the subtree
     * originating at this node in the trie structure.
     */
    val childrenJoin: UniquenessType

    /**
     * Represents the least upper bound (LUB) of uniqueness levels for the path from the current node
     * to the root of the trie structure.
     *
     * For example, for the node representing `x.y.z`, this would return the LUB of `x.y.z`, `x.y` and `x`.
     */
    val parentsJoin: UniquenessType
}