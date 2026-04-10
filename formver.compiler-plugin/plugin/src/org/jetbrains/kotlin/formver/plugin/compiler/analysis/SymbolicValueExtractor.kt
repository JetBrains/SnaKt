package org.jetbrains.kotlin.formver.plugin.compiler.analysis

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class SymbolicValueExtractor<T : SymbolicValue<T>> : FirVisitor<T, Unit>() {
    abstract val empty: T

    abstract fun FirBasedSymbol<*>.extract(): T

    private fun FirExpression?.visit(): T =
        this?.accept(this@SymbolicValueExtractor, Unit) ?: empty

    fun extract(expression: FirExpression): T {
        return expression.visit()
    }

    override fun visitElement(
        element: FirElement,
        data: Unit
    ): T {
        return empty
    }

    override fun visitBlock(
        block: FirBlock,
        data: Unit
    ): T {
        val lastValue = block.statements.lastOrNull() as? FirExpression
            ?: return empty

        return lastValue.visit()
    }

    override fun visitWhenExpression(
        whenExpression: FirWhenExpression,
        data: Unit
    ): T {
        return whenExpression.branches.fold(empty) { result, branch ->
            result.join(branch.result.visit())
        }
    }

    override fun visitElvisExpression(
        elvisExpression: FirElvisExpression,
        data: Unit
    ): T {
        return elvisExpression.lhs.visit().join(elvisExpression.rhs.visit())
    }

    override fun visitSmartCastExpression(
        smartCastExpression: FirSmartCastExpression,
        data: Unit
    ): T {
        return smartCastExpression.originalExpression.visit()
    }

    override fun visitWrappedExpression(
        wrappedExpression: FirWrappedExpression,
        data: Unit
    ): T {
        return wrappedExpression.expression.visit()
    }

    override fun visitCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: Unit
    ): T {
        return checkNotNullCall.argumentList.arguments.singleOrNull().visit()
    }

    private fun FirTypeOperatorCall.isCast(): Boolean =
        operation == FirOperation.AS || operation == FirOperation.SAFE_AS

    override fun visitTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: Unit
    ): T {
        if (!typeOperatorCall.isCast()) return empty

        return typeOperatorCall.argumentList.arguments.singleOrNull().visit()
    }

    override fun visitReturnExpression(
        returnExpression: FirReturnExpression,
        data: Unit
    ): T {
        return empty
    }

    override fun visitThrowExpression(
        throwExpression: FirThrowExpression,
        data: Unit
    ): T {
        return empty
    }

    override fun visitTryExpression(
        tryExpression: FirTryExpression,
        data: Unit
    ): T {
        val tryValue = tryExpression.tryBlock.visit()
        val catchValues = tryExpression.catches.fold(empty) { result, catch ->
            result.join(catch.block.visit())
        }

        return tryValue.join(catchValues)
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Unit
    ): T {
        val receiverValue = qualifiedAccessExpression.explicitReceiver.visit()
        val symbol = qualifiedAccessExpression.calleeReference.symbol
            ?: return empty

        return receiverValue.append(symbol.extract())
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: Unit
    ): T {
        val symbol = propertyAccessExpression.calleeReference.symbol
            ?: return empty

        return symbol.extract()
    }

    override fun visitFunctionCall(
        functionCall: FirFunctionCall,
        data: Unit
    ): T {
        return empty
    }

    override fun visitSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: Unit
    ): T {
        val receiverValue = safeCallExpression.receiver.visit()
        val selectorValue = (safeCallExpression.selector as? FirExpression).visit()

        return receiverValue.append(selectorValue)
    }

    override fun visitLiteralExpression(
        literalExpression: FirLiteralExpression,
        data: Unit
    ): T {
        return LiteralSymbol.extract()
    }
}
