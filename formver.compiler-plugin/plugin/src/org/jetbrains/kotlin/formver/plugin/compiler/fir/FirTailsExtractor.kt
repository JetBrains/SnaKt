package org.jetbrains.kotlin.formver.plugin.compiler.fir

import org.jetbrains.kotlin.fir.FirContractViolation
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirExpressionRef
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCheckedSafeCallSubject
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildCheckedSafeCallSubject
import org.jetbrains.kotlin.fir.expressions.builder.buildCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCallCopy
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpressionCopy
import org.jetbrains.kotlin.fir.expressions.builder.buildSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildTypeOperatorCall
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class FirTailsExtractor(
    val session: FirSession
) : FirVisitor<Sequence<FirExpression>, Unit>() {
    private fun FirElement.visit(): Sequence<FirExpression> {
        val cache = session.firTailsCache
        cache[this]?.let { return@visit it }
        val tails = accept(this@FirTailsExtractor, Unit)
        cache[this] = tails

        return tails
    }

    fun extract(element: FirElement): Sequence<FirExpression> {
        return element.visit()
    }

    override fun visitElement(
        element: FirElement,
        data: Unit
    ): Sequence<FirExpression> {
        return when (element) {
            is FirExpression -> sequenceOf(element)
            else -> emptySequence()
        }
    }

    override fun visitBlock(
        block: FirBlock,
        data: Unit
    ): Sequence<FirExpression> {
        val lastExpression = block.statements.lastOrNull() as? FirExpression
            ?: return emptySequence()

        return lastExpression.visit()
    }

    override fun visitWhenExpression(
        whenExpression: FirWhenExpression,
        data: Unit
    ): Sequence<FirExpression> {
        return whenExpression.branches.asSequence()
            .flatMap { branch -> branch.result.visit() }
    }

    override fun visitElvisExpression(
        elvisExpression: FirElvisExpression,
        data: Unit
    ): Sequence<FirExpression> {
        return elvisExpression.lhs.visit() + elvisExpression.rhs.visit()
    }

    override fun visitSmartCastExpression(
        smartCastExpression: FirSmartCastExpression,
        data: Unit
    ): Sequence<FirExpression> {
        return smartCastExpression.originalExpression.visit()
    }

    override fun visitWrappedExpression(
        wrappedExpression: FirWrappedExpression,
        data: Unit
    ): Sequence<FirExpression> {
        return wrappedExpression.expression.visit()
    }

    override fun visitCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: Unit
    ): Sequence<FirExpression> {
        val argument = checkNotNullCall.argumentList.arguments.singleOrNull()
            ?: return sequenceOf(checkNotNullCall)

        return argument.visit().map{ newArgument ->
            buildCheckNotNullCall {
                source = checkNotNullCall.source
                coneTypeOrNull = checkNotNullCall.resolvedType
                annotations += checkNotNullCall.annotations
                argumentList = buildArgumentList {
                    source = checkNotNullCall.argumentList.source
                    arguments += newArgument
                }
                calleeReference = checkNotNullCall.calleeReference
            }
        }
    }

    override fun visitTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: Unit
    ): Sequence<FirExpression> {
        val argument = typeOperatorCall.argumentList.arguments.singleOrNull()
            ?: return sequenceOf(typeOperatorCall)

        return argument.visit().map { newArgument ->
            buildTypeOperatorCall {
                source = typeOperatorCall.source
                coneTypeOrNull = typeOperatorCall.resolvedType
                annotations += typeOperatorCall.annotations
                operation = typeOperatorCall.operation
                conversionTypeRef = typeOperatorCall.conversionTypeRef
                argumentList = buildArgumentList {
                    source = typeOperatorCall.argumentList.source
                    arguments += newArgument
                }
            }
        }
    }

    override fun visitReturnExpression(
        returnExpression: FirReturnExpression,
        data: Unit
    ): Sequence<FirExpression> {
        return returnExpression.result.visit()
    }

    override fun visitTryExpression(
        tryExpression: FirTryExpression,
        data: Unit
    ): Sequence<FirExpression> {
        val tryTails = tryExpression.tryBlock.visit()
        val catchTails = tryExpression.catches.asSequence().flatMap { catch ->
            catch.block.visit()
        }

        return tryTails + catchTails
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Unit
    ): Sequence<FirExpression> {
        val receiver = qualifiedAccessExpression.explicitReceiver
            ?: return sequenceOf(qualifiedAccessExpression)

        return receiver.visit().map { newReceiver ->
            qualifiedAccessExpression.withExplicitReceiver(newReceiver)
        }
    }

    override fun visitFunctionCall(
        functionCall: FirFunctionCall,
        data: Unit
    ): Sequence<FirExpression> {
        val receiver = functionCall.explicitReceiver
            ?: return sequenceOf(functionCall)

        return receiver.visit().map { newReceiver ->
            functionCall.withExplicitReceiver(newReceiver)
        }
    }

    @OptIn(FirContractViolation::class)
    override fun visitSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: Unit
    ): Sequence<FirExpression> {
        return safeCallExpression.receiver.visit().map { newReceiver ->
            val checkedSubject = buildCheckedSafeCallSubject {
                source = safeCallExpression.source
                coneTypeOrNull = newReceiver.resolvedType
                originalReceiverRef = FirExpressionRef<FirExpression>()
                    .apply { bind(newReceiver) }
            }
            val newCheckedSubjectRef = FirExpressionRef<FirCheckedSafeCallSubject>()
                .apply { bind(checkedSubject) }
            val newSelector = (safeCallExpression.selector as? FirQualifiedAccessExpression)
                ?.withExplicitReceiver(checkedSubject)
                ?: safeCallExpression.selector

            buildSafeCallExpression {
                source = safeCallExpression.source
                coneTypeOrNull = safeCallExpression.resolvedType
                annotations += safeCallExpression.annotations
                receiver = newReceiver
                checkedSubjectRef = newCheckedSubjectRef
                selector = newSelector
            }
        }
    }

    private fun FirQualifiedAccessExpression.withExplicitReceiver(
        receiver: FirExpression
    ): FirExpression {
        return when (this) {
            is FirFunctionCall -> buildFunctionCallCopy(this) {
                explicitReceiver = receiver
            }

            is FirPropertyAccessExpression -> buildPropertyAccessExpressionCopy(this) {
                explicitReceiver = receiver
            }

            else -> error("Unsupported qualified access expression: ${this::class.simpleName}")
        }
    }
}
