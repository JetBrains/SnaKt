package org.jetbrains.kotlin.formver.plugin.compiler.analysis

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
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
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * Evaluates the possible variable/property paths yielded by an expression.
 */
abstract class FirExpressionSymbolicEvaluator<T> : FirVisitor<PathTrie<T>, Unit>() {

    abstract val default: PathTrie<T>

    abstract fun resolve(symbol: FirBasedSymbol<*>): T

    private fun FirExpression?.visit(): PathTrie<T> =
        this?.accept(this@FirExpressionSymbolicEvaluator, Unit) ?: default

    fun extract(expression: FirExpression): PathTrie<T> {
        return expression.visit()
    }

    override fun visitElement(
        element: FirElement,
        data: Unit
    ): PathTrie<T> {
        return default
    }

    private fun FirQualifiedAccessExpression.pathSymbol(): FirBasedSymbol<*>? {
        val symbol = calleeReference.symbol

        return symbol.takeIf { it is FirVariableSymbol<*> }
    }

    private fun PathTrie<T>.append(suffix: PathTrie<T>): PathTrie<T> {
        if (suffix.children.isEmpty()) return this

        return if (children.isEmpty()) {
            copy(children = suffix.children)
        } else {
            copy(children = children.mapValues { (_, child) -> child.append(suffix) })
        }
    }

    override fun visitBlock(
        block: FirBlock,
        data: Unit
    ): PathTrie<T> {
        val lastValue = block.statements.lastOrNull() as? FirExpression
            ?: return default

        return lastValue.visit()
    }

    override fun visitWhenExpression(
        whenExpression: FirWhenExpression,
        data: Unit
    ): PathTrie<T> {
        return whenExpression.branches.fold(default) { result, branch ->
            result.join(branch.result.visit())
        }
    }

    override fun visitElvisExpression(
        elvisExpression: FirElvisExpression,
        data: Unit
    ): PathTrie<T> {
        return elvisExpression.lhs.visit().join(elvisExpression.rhs.visit())
    }

    override fun visitSmartCastExpression(
        smartCastExpression: FirSmartCastExpression,
        data: Unit
    ): PathTrie<T> {
        return smartCastExpression.originalExpression.visit()
    }

    override fun visitWrappedExpression(
        wrappedExpression: FirWrappedExpression,
        data: Unit
    ): PathTrie<T> {
        return wrappedExpression.expression.visit()
    }

    override fun visitCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: Unit
    ): PathTrie<T> {
        return checkNotNullCall.argumentList.arguments.singleOrNull().visit()
    }

    private fun FirTypeOperatorCall.isCast(): Boolean =
        operation == FirOperation.AS || operation == FirOperation.SAFE_AS

    override fun visitTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: Unit
    ): PathTrie<T> {
        if (!typeOperatorCall.isCast()) return default

        return typeOperatorCall.argumentList.arguments.singleOrNull().visit()
    }

    override fun visitReturnExpression(
        returnExpression: FirReturnExpression,
        data: Unit
    ): PathTrie<T> {
        return default
    }

    override fun visitThrowExpression(
        throwExpression: FirThrowExpression,
        data: Unit
    ): PathTrie<T> {
        return default
    }

    override fun visitTryExpression(
        tryExpression: FirTryExpression,
        data: Unit
    ): PathTrie<T> {
        val tryValue = tryExpression.tryBlock.visit()
        val catchValues = tryExpression.catches.fold(default) { result, catch ->
            result.join(catch.block.visit())
        }

        return tryValue.join(catchValues)
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Unit
    ): PathTrie<T> {
        val symbol = qualifiedAccessExpression.pathSymbol()
            ?: return default
        val receiverValue = qualifiedAccessExpression.explicitReceiver
            ?: return default.ensure(sequenceOf(symbol)) {
                resolve(it)
            }

        return receiverValue.visit().append(
            default.ensure(sequenceOf(symbol)) {
                resolve(it)
            }
        )
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: Unit
    ): PathTrie<T> {
        val symbol = propertyAccessExpression.calleeReference.symbol
            ?: return default

        return default.ensure(sequenceOf(symbol)) {
            resolve(it)
        }
    }

    override fun visitFunctionCall(
        functionCall: FirFunctionCall,
        data: Unit
    ): PathTrie<T> {
        return default
    }

    override fun visitSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: Unit
    ): PathTrie<T> {
        val receiverValue = safeCallExpression.receiver.visit()
        val selectorValue = (safeCallExpression.selector as? FirExpression).visit()

        return receiverValue.append(selectorValue)
    }
}
