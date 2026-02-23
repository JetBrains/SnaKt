package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol

class UniquenessPathTrie(
    val parent: UniquenessPathTrie?,
    val symbol: FirBasedSymbol<*>?,
    val children: MutableMap<FirBasedSymbol<*>, UniquenessPathTrie>,
    override var type: UniquenessType
) : UniquenessPathStore {

    context(context: UniquenessResolver) override fun ensurePath(path: List<FirBasedSymbol<*>>): UniquenessPathTrie {
        if (path.isEmpty()) return this

        val (head, tail) = path.first() to path.drop(1)
        val node = children.getOrPut(head) {
            UniquenessPathTrie(this, head, mutableMapOf(), context.resolveUniquenessType(head))
        }

        return node.ensurePath(tail)
    }

    private val localVariable: FirBasedSymbol<*>?
        get() {
            val local = parent?.localVariable ?: this.symbol
            return local as? FirValueParameterSymbol
        }

    override val childrenJoin: UniquenessType
        get() = children.values.fold(type) { acc, child -> acc.join(child.childrenJoin) }

    override val parentsJoin: UniquenessType
        get() = parent?.parentsJoin?.join(type) ?: type

}
