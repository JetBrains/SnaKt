package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.common.ErrorCollector

class UniquenessExpressionChecker(
    val resolver: UniquenessResolver,
    val out: ErrorCollector
) : FirVisitor<Unit, Map<Path, UniquenessType>>() {

    fun check(store: Map<Path, UniquenessType>, expression: FirElement) =
        expression.accept(this, store)

    override fun visitElement(element: FirElement, data: Map<Path, UniquenessType>) {}

    @OptIn(SymbolInternals::class)
    override fun visitFunctionCall(functionCall: FirFunctionCall, data: Map<Path, UniquenessType>) {
        val callableSymbol = (functionCall.toResolvedCallableSymbol() as FirFunctionSymbol<*>).fir

        for ((argument, parameter) in functionCall.arguments.zip(callableSymbol.valueParameters)) {
            val argumentPath = argument.toPath()

            if (argumentPath != null) {
                val argumentType = data[argumentPath]
                val parameterType = resolver.resolveUniquenessType(parameter)

                if (parameterType is UniquenessType.Active) {
                    if (argumentType is UniquenessType.Moved) {
                        throw UniquenessCheckException(
                            argument.source,
                            "cannot access expression as its uniqueness state is top"
                        )
                    }

                    // TODO: Handle partially shared borrowed arguments

                    argumentType as UniquenessType.Active

                    if (argumentType.borrowLevel > parameterType.borrowLevel) {
                        throw UniquenessCheckException(
                            argument.source,
                            "cannot pass borrowed value as non-borrowed"
                        )
                    }

                    if (parameterType.uniqueLevel == UniqueLevel.Unique) {
                        if (argumentType.uniqueLevel != UniqueLevel.Unique) {
                            throw UniquenessCheckException(
                                argument.source,
                                "expected unique value, got ${argumentType.uniqueLevel.toString().lowercase()}"
                            )
                        }

                        // TODO: Handle partially shared unique arguments
                    }
                }
            }
        }
    }

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: Map<Path, UniquenessType>) {
        val resultPath = returnExpression.result.toPath()

        if (resultPath != null && data[resultPath] == UniquenessType.Moved) {
            throw UniquenessCheckException(returnExpression.source, "Returned value was moved")
        }
    }

    override fun visitThrowExpression(throwExpression: FirThrowExpression, data: Map<Path, UniquenessType>) {
        val thrownPath = throwExpression.exception.toPath()

        if (thrownPath != null && data[thrownPath] == UniquenessType.Moved) {
            throw UniquenessCheckException(throwExpression.source, "Thrown exception was moved")
        }
    }

}