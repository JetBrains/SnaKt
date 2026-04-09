package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.FirExpressionSymbolicValueExtractor

class FirExpressionLocalityEvaluator(
    private val context: CheckerContext
) : FirExpressionSymbolicValueExtractor<ConeLocalityAttribute?>() {
    override val empty: ConeLocalityAttribute? = null

    @OptIn(SymbolInternals::class)
    override fun create(symbol: FirBasedSymbol<*>): ConeLocalityAttribute? {
        val element = symbol.fir

        with(context) {
            return when (element) {
                is FirVariable -> element.localityAttribute
                else -> null
            }
        }
    }

    override fun ConeLocalityAttribute?.join(other: ConeLocalityAttribute?): ConeLocalityAttribute? {
        return this.union(other)
    }

    override fun ConeLocalityAttribute?.append(other: ConeLocalityAttribute?): ConeLocalityAttribute? {
        return this
    }
}

context(context: CheckerContext)
val FirExpression.localityAttribute: ConeLocalityAttribute?
    get() = FirExpressionLocalityEvaluator(context).extract(this)
