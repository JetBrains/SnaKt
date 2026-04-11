package org.jetbrains.kotlin.formver.plugin.compiler.analysis

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedExpression
import org.jetbrains.kotlin.fir.lastExpression
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class TailValueExtractor<T, D> : FirVisitor<T, D>() {
    abstract val empty: T

    abstract fun T.join(other: T): T

    protected fun FirExpression?.visit(data: D): T =
        this?.accept(this@TailValueExtractor, data) ?: empty

    fun extract(
        expression: FirExpression,
        data: D
    ): T {
        return expression.visit(data)
    }

    override fun visitElement(
        element: FirElement,
        data: D
    ): T {
        return empty
    }

    override fun visitBlock(
        block: FirBlock,
        data: D
    ): T {
        return block.lastExpression?.visit(data) ?: empty
    }

    override fun visitWhenExpression(
        whenExpression: FirWhenExpression,
        data: D
    ): T {
        return whenExpression.branches.fold(empty) { result, branch ->
            result.join(branch.result.visit(data))
        }
    }

    override fun visitElvisExpression(
        elvisExpression: FirElvisExpression,
        data: D
    ): T {
        return elvisExpression.lhs.visit(data).join(elvisExpression.rhs.visit(data))
    }

    override fun visitSmartCastExpression(
        smartCastExpression: FirSmartCastExpression,
        data: D
    ): T {
        return smartCastExpression.originalExpression.visit(data)
    }

    override fun visitWrappedExpression(
        wrappedExpression: FirWrappedExpression,
        data: D
    ): T {
        return wrappedExpression.expression.visit(data)
    }

    override fun visitCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: D
    ): T {
        return checkNotNullCall.argumentList.arguments.singleOrNull().visit(data)
    }

    private fun FirTypeOperatorCall.isCast(): Boolean =
        operation == FirOperation.AS || operation == FirOperation.SAFE_AS

    override fun visitTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: D
    ): T {
        if (!typeOperatorCall.isCast()) return empty

        return typeOperatorCall.argumentList.arguments.singleOrNull().visit(data)
    }

    override fun visitReturnExpression(
        returnExpression: FirReturnExpression,
        data: D
    ): T {
        return empty
    }

    override fun visitThrowExpression(
        throwExpression: FirThrowExpression,
        data: D
    ): T {
        return empty
    }

    override fun visitTryExpression(
        tryExpression: FirTryExpression,
        data: D
    ): T {
        val tryValue = tryExpression.tryBlock.visit(data)
        val catchValues = tryExpression.catches.fold(empty) { result, catch ->
            result.join(catch.block.visit(data))
        }

        return tryValue.join(catchValues)
    }

    override fun visitFunctionCall(
        functionCall: FirFunctionCall,
        data: D
    ): T {
        return empty
    }
}
