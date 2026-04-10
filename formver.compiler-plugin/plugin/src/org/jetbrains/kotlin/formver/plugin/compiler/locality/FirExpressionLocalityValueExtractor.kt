package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.FirExpressionSymbolicValueExtractor

class FirExpressionLocalityValueExtractor(
    context: CheckerContext
) : FirExpressionSymbolicValueExtractor<LocalityValue>(
    LocalityValue.Global,
    LocalityValueFactory(context)
)

context(context: CheckerContext)
val FirExpression.localityAttribute: LocalityValue
    get() = FirExpressionLocalityValueExtractor(context).extract(this)
