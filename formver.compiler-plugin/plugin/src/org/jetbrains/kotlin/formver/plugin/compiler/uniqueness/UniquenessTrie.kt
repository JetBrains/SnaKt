package org.jetbrains.kotlin.formver.plugin.compiler.uniqueness

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

class UniquenessTrie(
    element: Uniqueness,
    children: Map<FirBasedSymbol<*>, PathTrie<Uniqueness>> = emptyMap(),
) : PathTrie<Uniqueness>(element, children) {
    override fun Uniqueness.join(other: Uniqueness): Uniqueness =
        this.join(other)

    override fun construct(
        element: Uniqueness,
        children: Map<FirBasedSymbol<*>, PathTrie<Uniqueness>>
    ): PathTrie<Uniqueness> {
        return UniquenessTrie(element, children)
    }
}