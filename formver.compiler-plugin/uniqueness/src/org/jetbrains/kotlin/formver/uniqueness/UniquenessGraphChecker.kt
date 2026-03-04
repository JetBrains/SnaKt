/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
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
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
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

    private fun checkValueUsage(value: FirExpression, expectedType: UniquenessType.Active, data: UniquenessTrie) {
        for (valuePath in value.valuePaths) {
            val valueData = data.ensure(valuePath)
            val actualType = valueData.parentsJoin

            when (actualType) {
                // TODO: Use errorCollector for errors, allowing to report more than one error per declaration.
                is UniquenessType.Moved -> {
                    throw UniquenessCheckException(
                        value.source,
                        "Cannot pass a moved argument"
                    )
                }

                is UniquenessType.Active -> {
                    if (actualType.borrowLevel > expectedType.borrowLevel) {
                        throw UniquenessCheckException(
                            value.source,
                            "Expected ${expectedType.borrowLevel.toString().lowercase()} value, " +
                                    "got ${actualType.borrowLevel.toString().lowercase()}"
                        )
                    }

                    if (actualType.uniqueLevel > expectedType.uniqueLevel) {
                        throw UniquenessCheckException(
                            value.source,
                            "Expected ${expectedType.uniqueLevel.toString().lowercase()} value, " +
                                    "got ${actualType.uniqueLevel.toString().lowercase()}"
                        )
                    }
                }
            }

            if (actualType.uniqueLevel == UniqueLevel.Unique && !data.isInvariant(valuePath)) {
                val valuePartialType = valueData.childrenJoin

                when (valuePartialType) {
                    is UniquenessType.Moved ->
                        throw UniquenessCheckException(
                            value.source,
                            "Cannot pass a partially moved argument"
                        )

                    is UniquenessType.Active -> {
                        if (valuePartialType.uniqueLevel > expectedType.uniqueLevel &&
                            expectedType.uniqueLevel == UniqueLevel.Unique) {
                            throw UniquenessCheckException(
                                value.source,
                                "Cannot pass a partially shared argument as unique"
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(SymbolInternals::class)
    override fun visitFunctionCallEnterNode(node: FunctionCallEnterNode, data: UniquenessTrie) {
        val functionCall = node.fir
        val callableSymbol = functionCall.toResolvedCallableSymbol()
            ?: throw IllegalStateException("Unable to resolve ${functionCall}")
        val callableDeclaration = callableSymbol.fir as? FirSimpleFunction
            ?: throw IllegalStateException("Callable symbol is not a function: ${callableSymbol.fir}")

        var currentData = data

        for ((argument, parameter) in functionCall.arguments.zip(callableDeclaration.valueParameters)) {
            val argumentPaths = argument.valuePaths

            for (argumentPath in argumentPaths) {
                val parameterType = resolver.resolveUniquenessType(parameter.symbol)
                checkValueUsage(argument, parameterType, currentData)

                // TODO: Look into how to do this at the analyzer level.
                //  As of now the analysis can only provide us with the uniqueness `data` before and after the method
                //  call, but we would like to compute this data before and after each parameter reference.
                //  {@see UniquenessGraphAnalyzer.visitFunctionCallEnterNode}
                if (parameterType.uniqueLevel == UniqueLevel.Unique ||
                    parameterType.borrowLevel == BorrowLevel.Local) {
                    currentData = currentData.copy()
                    currentData[argumentPath] = UniquenessType.Moved
                }
            }
        }
    }

    private fun checkLeakedValue(value: FirExpression, operation: String, data: UniquenessTrie) {
        val valuePaths = value.valuePaths

        for (valuePath in valuePaths) {
            val valueType = data.ensure(valuePath).parentsJoin

            when (valueType) {
                is UniquenessType.Active -> {
                    if (valueType.borrowLevel == BorrowLevel.Local) {
                        throw UniquenessCheckException(
                            value.source,
                            "Cannot ${operation} a borrowed value"
                        )
                    }
                }

                is UniquenessType.Moved -> {
                    throw UniquenessCheckException(
                        value.source,
                        "Cannot ${operation} a moved value"
                    )
                }
            }
        }
    }

    override fun visitJumpNode(node: JumpNode, data: UniquenessTrie) {
        val returnExpression = node.fir as? FirReturnExpression ?: return
        val value = returnExpression.result
        checkLeakedValue(value, "return", data)
    }

    override fun visitThrowExceptionNode(node: ThrowExceptionNode, data: UniquenessTrie) {
        val throwExpression = node.fir
        val exception = throwExpression.exception
        checkLeakedValue(exception, "throw", data)
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