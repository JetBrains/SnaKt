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
 * Visitor assigning uniqueness types to CFG nodes.
 *
 * @param resolver The resolver used for fetching the default uniqueness type of some path.
 */
class UniquenessTypeAssigner(
    private val resolver: UniquenessResolver
): ControlFlowGraphVisitor<UniquenessTrie, UniquenessTrie>() {

    override fun visitNode(node: CFGNode<*>, data: UniquenessTrie): UniquenessTrie {
        return data
    }

    private fun handleDefinition(assignee: FirElement, assigned: FirElement, data: UniquenessTrie): UniquenessTrie {
        val result = data.copy()
        val left = assignee.toPath()

        if (left != null) {
            val right = assigned.toPath()

            if (right != null) {
                // Path-to-Path assignment
                val rightType = data[right]

                result[left] = rightType

                if (rightType is UniquenessType.Active &&
                    (rightType.uniqueLevel == UniqueLevel.Unique || rightType.borrowLevel == BorrowLevel.Borrowed)) {
                    result[right] = UniquenessType.Moved
                    result[left] = rightType
                }

                // TODO: Handle call assignments
            } else {
                // Generic value assignment
                result[left] = UniquenessType.Active(UniqueLevel.Shared, BorrowLevel.Consumed)
            }
        }

        return result
    }

    override fun visitVariableDeclarationNode(node: VariableDeclarationNode, data: UniquenessTrie): UniquenessTrie {
        val assignee = node.fir
        val assigned = node.fir.initializer

        if (assigned == null) {
            return data
        }

        return handleDefinition(assignee, assigned, data)
    }

    override fun visitVariableAssignmentNode(node: VariableAssignmentNode, data: UniquenessTrie): UniquenessTrie {
        val assignee = node.fir.lValue
        val assigned = node.fir.rValue

        return handleDefinition(assignee, assigned, data)
    }

    @OptIn(SymbolInternals::class)
    override fun visitFunctionCallEnterNode(node: FunctionCallEnterNode, data: UniquenessTrie): UniquenessTrie {
        val call = node.fir
        val callableSymbol = (call.toResolvedCallableSymbol() as FirFunctionSymbol<*>).fir
        val result = data.copy()

        for ((argument, parameter) in call.arguments.zip(callableSymbol.valueParameters)) {
            val right = argument.toPath()

            if (right != null) {
                val leftType = resolver.resolveUniquenessType(parameter.symbol)

                if (leftType.borrowLevel == BorrowLevel.Consumed) {
                    when (leftType.uniqueLevel) {
                        UniqueLevel.Unique -> {
                            result[right] = UniquenessType.Moved
                        }
                        UniqueLevel.Shared -> {
                            result[right] = UniquenessType.Active(UniqueLevel.Shared, BorrowLevel.Consumed)
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
