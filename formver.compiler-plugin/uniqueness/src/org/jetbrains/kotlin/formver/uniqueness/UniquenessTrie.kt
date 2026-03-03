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
class UniquenessTrie(
    val resolver: UniquenessResolver,
    var type: UniquenessType = UniquenessType.Active(UniqueLevel.Unique, BorrowLevel.Consumed),
    private val children: MutableMap<FirBasedSymbol<*>, UniquenessTrie> = mutableMapOf(),
    private val parent: UniquenessTrie? = null
) {

    /**
     * Represents the least upper bound (LUB) of uniqueness levels for the path from the current node * to the root of
     * the trie structure.
     *
     * For example, for the node representing `x.y.z`, this would return the LUB of `x.y.z`, `x.y` and `x`.
     */
    val parentsJoin: UniquenessType
        get() =
            if (parent != null)
                type.join(parent.parentsJoin)
            else
                type

    /**
     * Represents the least upper bound (LUB) of uniqueness levels for the subtree * originating at this node in the
     * trie structure.
     */
    val childrenJoin: UniquenessType
        get() = children.values.fold(type) { result, child ->
            result.join(child.childrenJoin)
        }

    private fun isInvariant(symbol: FirBasedSymbol<*>): Boolean {
        val actual = type
        val default = resolver.resolveUniquenessType(symbol)

        return actual == default &&
                children.all { (symbol, store) ->
                    store.isInvariant(symbol)
                }
    }

    /**
     * @return true if the subpaths of `symbol` are invariant with respect to their default specification, `false`
     * otherwise.
     */
    fun isInvariant(path: Path): Boolean {
        return path.isEmpty() || ensure(path).isInvariant(path.first())
    }

    /**
     * Ensures that there is a `UniquenessTrie` associated with the specified path, creating one if it does not exist.
     *
     * @param path The path for which the uniqueness store is to be ensured.
     * @return The `UniquenessTrie` root corresponding to the given path, creating and associating one if necessary.
     */
    fun ensure(path: Path): UniquenessTrie {
        if (path.isEmpty()) {
            return this
        }

        val head = path.first()
        val foundChild = children[head]
        val child: UniquenessTrie

        if (foundChild == null) {
            child = UniquenessTrie(
                resolver = resolver,
                parent = this
            )
            child.type = resolver.resolveUniquenessType(head)
            children[head] = child
        } else {
            child = foundChild
        }

        return child.ensure(path.subList(1, path.size))
    }

    /**
     * Retrieves the uniqueness type for a given path.
     *
     * @param path The path to retrieve the uniqueness type for.
     * @return The uniqueness type for the given path, or null if not found.
     */
    operator fun get(path: Path): UniquenessType {
        return ensure(path).type
    }

    /**
     * Sets the uniqueness type for a given path.
     *
     * @param path The path to set the uniqueness type for.
     * @return The updated UniquenessStore.
     */
    operator fun set(path: Path, value: UniquenessType) {
        val store = ensure(path)
        store.type = value
    }

    private fun copyWithParent(newParent: UniquenessTrie?): UniquenessTrie {
        val newNode = UniquenessTrie(resolver, type, mutableMapOf(), newParent)

        for ((key, child) in children) {
            newNode.children[key] = child.copyWithParent(newNode)
        }

        return newNode
    }

    /**
     * @return a deep copy of this UniquenessTrie.
     */
    fun copy(): UniquenessTrie {
        val newChildren = children.mapValuesTo(mutableMapOf()) { (_, child) ->
            child.copyWithParent(this)
        }

        return UniquenessTrie(resolver, type, newChildren, parent)
    }

    /**
     * In-place, pointwise join of two UniquenessTries.
     *
     * @param other The other UniquenessTrie to join with.
     */
    fun join(other: UniquenessTrie) {
        type = type.join(other.type)

        for ((key, otherChild) in other.children) {
            val thisChild = children[key]

            if (thisChild != null) {
                thisChild.join(otherChild)
            } else {
                children[key] = otherChild.copyWithParent(this)
            }
        }
    }

    private fun toString(prefix: String): String {
        val builder = StringBuilder()
        val prefix = "$prefix : $type"

        for ((key, child) in children) {
            builder.appendLine(child.toString("$prefix / $key"))
        }

        return builder.toString()
    }

    override fun toString(): String =
        toString("root")

    override fun equals(other: Any?): Boolean {
        return other is UniquenessTrie &&
                resolver == other.resolver &&
                children == other.children &&
                type == other.type
    }

    override fun hashCode(): Int {
        var result = resolver.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + children.hashCode()

        return result
    }

}