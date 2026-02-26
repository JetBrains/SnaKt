/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirSession
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
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.common.ErrorCollector

class UniquenessTypeChecker(
    val resolver: UniquenessResolver,
    val out: ErrorCollector
) : ControlFlowGraphVisitor<Unit, UniquenessTrie>() {

    override fun visitNode(node: CFGNode<*>, data: UniquenessTrie) {}

    @OptIn(SymbolInternals::class)
    override fun visitFunctionCallEnterNode(node: FunctionCallEnterNode, data: UniquenessTrie) {
        val functionCall = node.fir
        val callableSymbol = (functionCall.toResolvedCallableSymbol() as FirFunctionSymbol<*>).fir

        for ((argument, parameter) in functionCall.arguments.zip(callableSymbol.valueParameters)) {
            val argumentPath = argument.toPath()

            if (argumentPath != null) {
                val argumentStore = data.ensure(argumentPath)
                val argumentType = argumentStore.parentsJoin
                val parameterType = resolver.resolveUniquenessType(parameter.symbol)

                when (argumentType) {
                    is UniquenessType.Moved -> {
                        throw UniquenessCheckException(
                            argument.source,
                            "Cannot pass a moved argument"
                        )
                    }

                    is UniquenessType.Active -> {
                        if (argumentType.borrowLevel > parameterType.borrowLevel) {
                            throw UniquenessCheckException(
                                argument.source,
                                "Expected ${parameterType.borrowLevel.toString().lowercase()} value, " +
                                        "got ${argumentType.borrowLevel.toString().lowercase()}"
                            )
                        }

                        if (parameterType.uniqueLevel > argumentType.uniqueLevel) {
                            throw UniquenessCheckException(
                                argument.source,
                                "Expected ${parameterType.uniqueLevel.toString().lowercase()} value, " +
                                        "got ${argumentType.uniqueLevel.toString().lowercase()}"
                            )
                        }
                    }
                }

                val argumentPartialType = argumentStore.childrenJoin

                if (!data.isInvariant(argumentPath)) {
                    if (argumentPartialType is UniquenessType.Moved) {
                        throw UniquenessCheckException(
                            argument.source,
                            "Cannot pass a partially moved argument"
                        )
                    }

                    if (argumentPartialType is UniquenessType.Active
                        && argumentPartialType.uniqueLevel == UniqueLevel.Shared) {
                        throw UniquenessCheckException(
                            argument.source,
                            "Cannot pass a partially shared argument"
                        )
                    }
                }
            }
        }
    }

    override fun visitJumpNode(node: JumpNode, data: UniquenessTrie) {
        val returnExpression = node.fir

        if (returnExpression !is FirReturnExpression) {
            return
        }

        val leakedPath = returnExpression.result.toPath()

        if (leakedPath != null) {
            val leakedType = data[leakedPath]

            if (leakedType is UniquenessType.Moved) {
                throw UniquenessCheckException(returnExpression.source, "Cannot return a moved value")
            }

            leakedType as UniquenessType.Active

            if (leakedType.borrowLevel == BorrowLevel.Borrowed) {
                throw UniquenessCheckException(returnExpression.source, "Cannot return a borrowed value")
            }
        }
    }

    override fun visitThrowExceptionNode(node: ThrowExceptionNode, data: UniquenessTrie) {
        val throwExpression = node.fir
        val leakedPath = throwExpression.toPath()

        if (leakedPath != null) {
            val leakedType = data[leakedPath]

            if (leakedType is UniquenessType.Moved) {
                throw UniquenessCheckException(throwExpression.source, "Cannot throw a moved value")
            }

            leakedType as UniquenessType.Active

            if (leakedType.borrowLevel == BorrowLevel.Borrowed) {
                throw UniquenessCheckException(throwExpression.source, "Cannot throw a borrowed value")
            }
        }
    }

}

class UniquenessGraphChecker(
    session: FirSession,
    initial: UniquenessTrie,
    out: ErrorCollector
) {

    val resolver = UniquenessResolver(session)

    val analyzer = UniquenessGraphAnalyzer(resolver, initial)

    val typeChecker = UniquenessTypeChecker(resolver, out)

    fun check(graph: ControlFlowGraph) {
        val facts = analyzer.analyze(graph)

        for (node in graph.nodes) {
            val store = facts.flowBefore(node)
            node.accept(typeChecker, store)
        }
    }

}