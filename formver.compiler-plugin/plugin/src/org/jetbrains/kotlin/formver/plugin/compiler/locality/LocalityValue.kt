package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.PathTrie

class LocalityValue(
    element: ConeLocalAttribute? = null,
    children: Map<FirBasedSymbol<*>, PathTrie<ConeLocalAttribute?>> = emptyMap(),
) : PathTrie<ConeLocalAttribute?>(element, children) {

    override fun ConeLocalAttribute?.join(other: ConeLocalAttribute?): ConeLocalAttribute? {
        return this.union(other)
    }

    override fun construct(
        element: ConeLocalAttribute?,
        children: Map<FirBasedSymbol<*>, PathTrie<ConeLocalAttribute?>>
    ): PathTrie<ConeLocalAttribute?> = LocalityValue(element, children)
}

context(context: CheckerContext)
val FirExpression.resolvedLocalAttribute: ConeLocalAttribute?
    get() = localityValue.childrenJoin
