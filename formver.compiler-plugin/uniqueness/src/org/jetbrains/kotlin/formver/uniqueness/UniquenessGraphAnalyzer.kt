/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

/**
 * Visitor assigning uniqueness types to paths after the execution of a [CFGNode].
 *
 * @param resolver The resolver used for fetching the default uniqueness type of parameter symbols.
 */
class UniquenessTypeAssigner(
    private val resolver: UniquenessResolver
): ControlFlowGraphVisitor<UniquenessTrie, UniquenessTrie>() {
    override fun visitNode(node: CFGNode<*>, data: UniquenessTrie): UniquenessTrie = data

    private fun evaluateDefinition(receiver: FirElement, value: FirElement, data: UniquenessTrie): UniquenessTrie {
        val result = data.copy()
        val receiverPath = receiver.receiverPath
        val valuePaths = value.valuePaths

        if (receiverPath != null) {
            result[receiverPath] = resolver.resolveUniquenessType(receiverPath.last())
        }

        for (valuePath in valuePaths) {
            val valueType = data[valuePath]

            if (valueType is UniquenessType.Active && valueType.uniqueLevel == UniqueLevel.Unique) {
                result[valuePath] = UniquenessType.Moved
            }
        }

        return result
    }

    override fun visitVariableDeclarationNode(node: VariableDeclarationNode, data: UniquenessTrie): UniquenessTrie {
        val receiver = node.fir
        val value = node.fir.initializer ?: return data

        return evaluateDefinition(receiver, value, data)
    }

    override fun visitVariableAssignmentNode(node: VariableAssignmentNode, data: UniquenessTrie): UniquenessTrie {
        val receiver = node.fir.lValue
        val value = node.fir.rValue

        return evaluateDefinition(receiver, value, data)
    }

    @OptIn(SymbolInternals::class)
    override fun visitFunctionCallEnterNode(node: FunctionCallEnterNode, data: UniquenessTrie): UniquenessTrie {
        val functionCall = node.fir
        val callableSymbol = functionCall.toResolvedCallableSymbol() as? FirFunctionSymbol<*>
            ?: throw IllegalStateException("Unable to resolve ${functionCall}")
        val callableDeclaration = callableSymbol.fir
        val result = data.copy()

        // TODO: If possible, it would be good to compute the outflow for the argument before reaching the
        //  [FunctionCallEnterNode]. This would allow us to provide more precise flow information to the checker.
        //  {@see UniquenessGraphChecker.visitFunctionCallEnterNode}
        for ((argument, parameter) in functionCall.arguments.zip(callableDeclaration.valueParameters)) {
            for (argumentPath in argument.valuePaths) {
                val parameterType = resolver.resolveUniquenessType(parameter.symbol)

                if (parameterType.borrowLevel == BorrowLevel.Global) {
                    when (parameterType.uniqueLevel) {
                        UniqueLevel.Unique -> {
                            result[argumentPath] = UniquenessType.Moved
                        }

                        UniqueLevel.Shared -> {
                            result[argumentPath] = UniquenessType.Active(UniqueLevel.Shared, BorrowLevel.Global)
                        }
                    }
                }
            }
        }

        return result
    }
}

/**
 * Analyzer inferring the uniqueness typing environment before and after the execution of each node.
 *
 * @param resolver The resolver used for fetching the default uniqueness type of some path.
 * @param initial The initial uniqueness typing environment.
 */
class UniquenessGraphAnalyzer(
    resolver: UniquenessResolver,
    override val initial: UniquenessTrie
) : DataFlowAnalyzer<UniquenessTrie>(FlowDirection.FORWARD) {
    val typeAssigner = UniquenessTypeAssigner(resolver)

    override val bottom: UniquenessTrie = UniquenessTrie(resolver)

    override fun join(a: UniquenessTrie, b: UniquenessTrie): UniquenessTrie {
        val result = a.copy()
        result.join(b)

        return result
    }

    @OptIn(SymbolInternals::class)
    override fun transfer(node: CFGNode<*>, inFlow: UniquenessTrie): UniquenessTrie =
        node.accept(typeAssigner, inFlow)
}
