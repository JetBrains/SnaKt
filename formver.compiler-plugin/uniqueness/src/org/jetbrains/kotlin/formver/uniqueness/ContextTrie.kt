package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol

/**
 * Trie node that tracks ownership (uniqueness) state for a single access-path component
 * in the uniqueness checker.
 *
 * Each node represents one symbol in a dotted path such as `x.y.z`; the root node is
 * created by [UniqueChecker] and has `parent = null` and `symbol = null`.  Child nodes
 * are created lazily via [getOrPutPath] and carry the [UniqueLevel] derived from the
 * `@Unique` annotation on the corresponding declaration.
 *
 * The trie structure allows the checker to reason about partial moves: if `x.y` has been
 * moved (level = [UniqueLevel.Top]) while `x.z` has not, the [subtreeLUB] and
 * [pathToRootLUB] properties capture those combined states efficiently.
 *
 * @param parent The parent trie node, or `null` for the root.
 * @param symbol The [FirBasedSymbol] this node represents, or `null` for the root.
 * @param children Mutable map from a child symbol to its trie node.
 * @param level The current [UniqueLevel] of this node; may be mutated as ownership is transferred.
 */
class ContextTrie(
    val parent: ContextTrie?,
    val symbol: FirBasedSymbol<*>?,
    val children: MutableMap<FirBasedSymbol<*>, ContextTrie>,
    override var level: UniqueLevel
) : UniquePathContext {
    context(context: UniqueCheckerContext) override fun getOrPutPath(path: List<FirBasedSymbol<*>>): ContextTrie {
        if (path.isEmpty()) return this

        val (head, tail) = path.first() to path.drop(1)
        val node = children.getOrPut(head) {
            ContextTrie(this, head, mutableMapOf(), context.resolveUniqueAnnotation(head))
        }
        return node.getOrPutPath(tail)
    }

    private val localVariable: FirBasedSymbol<*>?
        get() {
            val local = parent?.localVariable ?: this.symbol
            return local as? FirValueParameterSymbol
        }

    context(context: UniqueCheckerContext) override val borrowingLevel: BorrowingLevel
        get() = localVariable?.let { context.resolveBorrowingAnnotation(it) } ?: BorrowingLevel.Plain

    override val subtreeLUB: UniqueLevel
        get() = listOfNotNull(
            level,
            children.values.maxOfOrNull { it.subtreeLUB }).max()

    override val pathToRootLUB: UniqueLevel
        get() = listOfNotNull(parent?.pathToRootLUB, level).max()

    context(context: UniqueCheckerContext) override val hasChanges: Boolean
        get() {
            val changed = symbol?.let { level != context.resolveUniqueAnnotation(it) } ?: false
            return changed || children.values.any { it.hasChanges }
        }
}
