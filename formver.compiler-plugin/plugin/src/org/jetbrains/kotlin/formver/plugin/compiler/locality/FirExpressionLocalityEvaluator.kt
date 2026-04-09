package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.FirExpressionSymbolicEvaluator
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.PathTrie

class FirExpressionLocalityEvaluator(
    private val context: CheckerContext
) : FirExpressionSymbolicEvaluator<ConeLocalAttribute?>() {
    @OptIn(SymbolInternals::class)
    override fun resolve(symbol: FirBasedSymbol<*>): ConeLocalAttribute? {
        val element = symbol.fir

        with(context) {
            return when (element) {
                is FirVariable -> element.resolvedLocalAttribute
                else -> null
            }
        }
    }

    override val default: PathTrie<ConeLocalAttribute?> = LocalityValue()
}

context(context: CheckerContext)
val FirExpression.localityValue: PathTrie<ConeLocalAttribute?>
    get() = FirExpressionLocalityEvaluator(context).extract(this)
