/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.JumpNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ThrowExceptionNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.common.ErrorCollector

/**
 * A visitor for checking uniqueness types in a [CFGNode].
 *
 * @param resolver The resolver used for fetching the default uniqueness type of parameter symbols.
 * @param errorCollector A collector for the checker's errors.
 */
class UniquenessTypeChecker(
    val resolver: UniquenessResolver,
    val errorCollector: ErrorCollector
) : ControlFlowGraphVisitor<Unit, UniquenessTrie>() {
    override fun visitNode(node: CFGNode<*>, data: UniquenessTrie) {}

    private fun checkValueUsage(
        value: FirExpression,
        expectedType: UniquenessType.Active,
        data: UniquenessTrie,
        onError: (UniquenessType) -> Unit
    ) {
        for (valuePath in value.valuePaths) {
            val valueData = data.ensure(valuePath)
            val actualType = valueData.parentsJoin

            when (actualType) {
                // TODO: Use errorCollector for errors, allowing to report more than one error per declaration.
                is UniquenessType.Moved -> {
                    return onError(actualType)
                }

                is UniquenessType.Active -> {
                    if (actualType.borrowLevel > expectedType.borrowLevel) {
                        return onError(actualType)
                    }

                    if (actualType.uniqueLevel > expectedType.uniqueLevel) {
                        return onError(actualType)
                    }
                }
            }
        }
    }

    private fun checkDefinition(
        receiver: FirElement,
        value: FirExpression,
        data: UniquenessTrie,
        onError: (UniquenessType, UniquenessType) -> Unit
    ) {
        val receiverPath = receiver.receiverPath ?: return
        val expectedType = resolver.resolveUniquenessType(receiverPath.last())
        checkValueUsage(value, expectedType, data) { actualType ->
            onError(expectedType, actualType)
        }
    }

    override fun visitVariableDeclarationNode(node: VariableDeclarationNode, data: UniquenessTrie) {
        val receiver = node.fir
        val value = node.fir.initializer ?: return
        checkDefinition(receiver, value, data) { expectedType, actualType ->
            throw UniquenessCheckException(
                value.source,
                "Initializer uniqueness mismatch: expected '${expectedType}', actual '${actualType}'."
            )
        }
    }

    override fun visitVariableAssignmentNode(node: VariableAssignmentNode, data: UniquenessTrie) {
        val receiver = node.fir.lValue
        val value = node.fir.rValue
        checkDefinition(receiver, value, data) { expectedType, actualType ->
            throw UniquenessCheckException(
                value.source,
                "Assignment uniqueness mismatch: expected '${expectedType}', actual '${actualType}'."
            )
        }
    }

    private fun checkInvariance(
        value: FirExpression,
        data: UniquenessTrie,
        onError: (UniquenessType, UniquenessType) -> Unit
    ) {
        for (valuePath in value.valuePaths) {
            val valueType = data.ensure(valuePath).parentsJoin
            val expectedType = resolver.resolveUniquenessType(valuePath.last())

            if (valueType is UniquenessType.Active &&
                valueType.uniqueLevel == UniqueLevel.Unique &&
                !data.isInvariant(valuePath)) {
                val valuePartialType = data.childrenJoin

                when (valuePartialType) {
                    is UniquenessType.Moved ->
                        return onError(valueType, valuePartialType)

                    is UniquenessType.Active -> {
                        if (valuePartialType.uniqueLevel > expectedType.uniqueLevel) {
                            return onError(valueType, valuePartialType)
                        }
                    }
                }
            }
        }
    }

    @OptIn(SymbolInternals::class)
    override fun visitFunctionCallEnterNode(node: FunctionCallEnterNode, data: UniquenessTrie) {
        val functionCall = node.fir
        val callableSymbol = functionCall.toResolvedCallableSymbol() as? FirFunctionSymbol<*>
            ?: throw IllegalStateException("Unable to resolve ${functionCall}")
        val callableDeclaration = callableSymbol.fir
        var currentData = data

        for ((argument, parameter) in functionCall.arguments.zip(callableDeclaration.valueParameters)) {
            val argumentPaths = argument.valuePaths

            for (argumentPath in argumentPaths) {
                val parameterType = resolver.resolveUniquenessType(parameter.symbol)
                checkValueUsage(argument, parameterType, currentData) { actualType ->
                    throw UniquenessCheckException(
                        argument.source,
                        "Argument uniqueness mismatch: expected '${parameterType}', actual '${actualType}'."
                    )
                }
                checkInvariance(argument, currentData) { expectedType, partialType ->
                    throw UniquenessCheckException(
                        argument.source,
                        "Argument uniqueness mismatch: partial type '${partialType}' is inconsistent with parent type '${expectedType}'."
                    )
                }

                // TODO: Look into how to do this at the analyzer level.
                //  As of now the analysis can only provide us with the uniqueness `data` before and after the method
                //  call, but we would like to compute this data before and after each parameter reference.
                //  {@see UniquenessGraphAnalyzer.visitFunctionCallEnterNode}
                if (parameterType.uniqueLevel == UniqueLevel.Unique) {
                    currentData = currentData.copy()
                    currentData.ensure(argumentPath).type = UniquenessType.Moved
                }
            }
        }
    }

    private fun checkLeakedValue(
        value: FirExpression,
        data: UniquenessTrie,
        onError: (UniquenessType) -> Unit
    ) {
        val valuePaths = value.valuePaths

        for (valuePath in valuePaths) {
            // TODO: Determine whether this is the only we we should be able to access the type of something
            val valueType = data.ensure(valuePath).parentsJoin

            when (valueType) {
                is UniquenessType.Moved -> {
                    return onError(valueType)
                }

                is UniquenessType.Active -> {
                    if (valueType.borrowLevel == BorrowLevel.Local) {
                        return onError(valueType)
                    }
                }
            }
        }
    }

    override fun visitJumpNode(node: JumpNode, data: UniquenessTrie) {
        val returnExpression = node.fir as? FirReturnExpression ?: return
        val value = returnExpression.result

        checkLeakedValue(value, data) { valueType ->
            throw UniquenessCheckException(
                value.source,
                "Return uniqueness mismatch: cannot return a '${valueType}' value"
            )
        }
    }

    override fun visitThrowExceptionNode(node: ThrowExceptionNode, data: UniquenessTrie) {
        val throwExpression = node.fir
        val exception = throwExpression.exception
        checkLeakedValue(exception, data) { valueType ->
            throw UniquenessCheckException(
                exception.source,
                "Throw uniqueness mismatch: cannot throw a '${valueType}' value"
            )
        }
    }
}

class UniquenessGraphChecker(
    session: FirSession,
    initial: UniquenessTrie,
    errorCollector: ErrorCollector
) {
    val resolver = UniquenessResolver(session)

    val analyzer = UniquenessGraphAnalyzer(resolver, initial)

    val typeChecker = UniquenessTypeChecker(resolver, errorCollector)

    fun check(graph: ControlFlowGraph) {
        val facts = analyzer.analyze(graph)

        for (node in graph.nodes) {
            val store = facts.flowBefore(node)
            node.accept(typeChecker, store)
        }
    }
}