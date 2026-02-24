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

class UniquenessTypeAssigner(
    private val resolver: UniquenessResolver
): ControlFlowGraphVisitor<Map<Path, UniquenessType>, Map<Path, UniquenessType>>() {

    override fun visitNode(node: CFGNode<*>, data: Map<Path, UniquenessType>): Map<Path, UniquenessType> {
        return data
    }

    private fun handleDefinition(assignee: FirElement, assigned: FirElement, data: Map<Path, UniquenessType>): Map<Path, UniquenessType> {
        val result = data.toMutableMap()

        // Handle assignments between locals
        val assigneePath = assignee.toPath()

        if (assigneePath != null) {
            val assignedPath = assigned.toPath()

            if (assignedPath != null) {
                // Path-to-Path assignment
                val assignedUniqueness = data[assignedPath];

                when (assignedUniqueness) {
                    UniquenessType.Active(UniqueLevel.Unique, BorrowLevel.Borrowed) -> {
                        result[assigneePath] = assignedUniqueness
                        result[assignedPath] = UniquenessType.Moved
                    }

                    UniquenessType.Active(UniqueLevel.Shared, BorrowLevel.Borrowed) -> {
                        result[assigneePath] = assignedUniqueness
                        result[assignedPath] = UniquenessType.Moved
                    }

                    UniquenessType.Active(UniqueLevel.Unique, BorrowLevel.Free) -> {
                        result[assigneePath] = UniquenessType.Active(UniqueLevel.Shared, BorrowLevel.Free)
                        result[assignedPath] = UniquenessType.Active(UniqueLevel.Shared, BorrowLevel.Free)
                    }

                    UniquenessType.Active(UniqueLevel.Shared, BorrowLevel.Free) -> {
                        result[assigneePath] = UniquenessType.Active(UniqueLevel.Shared, BorrowLevel.Free)
                    }

                    UniquenessType.Moved -> {
                        result[assigneePath] = UniquenessType.Moved
                    }

                    else -> {
                        throw IllegalStateException("Unexpected UniquenessType: ${data[assignedPath]}")
                    }
                }
            }

            // TODO: Handle call assignments
        }

        return result
    }

    override fun visitVariableDeclarationNode(node: VariableDeclarationNode, data: Map<Path, UniquenessType>): Map<Path, UniquenessType> {
        val assignee = node.fir
        val assigned = node.fir.initializer

        if (assigned == null) {
            return data
        }

        return handleDefinition(assignee, assigned, data)
    }

    override fun visitVariableAssignmentNode(node: VariableAssignmentNode, data: Map<Path, UniquenessType>): Map<Path, UniquenessType> {
        val assignee = node.fir.lValue
        val assigned = node.fir.rValue

        return handleDefinition(assignee, assigned, data)
    }

    @OptIn(SymbolInternals::class)
    override fun visitFunctionCallEnterNode(node: FunctionCallEnterNode, data: Map<Path, UniquenessType>): Map<Path, UniquenessType> {
        val call = node.fir
        val callableSymbol = (call.toResolvedCallableSymbol() as FirFunctionSymbol<*>).fir
        val result = data.toMutableMap()

        for ((argument, parameter) in call.arguments.zip(callableSymbol.valueParameters)) {
            val argumentPath = argument.toPath()

            if (argumentPath != null) {
                val argumentType = data[argumentPath]
                val parameterType = resolver.resolveUniquenessType(parameter)

                if (parameterType is UniquenessType.Active
                    && parameterType.borrowLevel == BorrowLevel.Free
                    && argumentType is UniquenessType.Active
                    && argumentType.borrowLevel == BorrowLevel.Borrowed) {
                    result[argumentPath] = UniquenessType.Moved
                }
            }
        }

        return result
    }

}

class UniquenessGraphAnalyzer(
    resolver: UniquenessResolver,
    override val initial: Map<Path, UniquenessType>
) : DataFlowAnalyzer<Map<Path, UniquenessType>>(FlowDirection.FORWARD) {

    val typeAssigner = UniquenessTypeAssigner(resolver)

    override val bottom: Map<Path, UniquenessType> = mapOf()

    override fun join(a: Map<Path, UniquenessType>, b: Map<Path, UniquenessType>): Map<Path, UniquenessType> {
        val result = a.toMutableMap()

        for ((bSymbol, bType) in b) {
            result.merge(bSymbol, bType) { existing, incoming -> existing.join(incoming) }
        }

        return result
    }

    @OptIn(SymbolInternals::class)
    override fun transfer( node: CFGNode<*>, inFlow: Map<Path, UniquenessType> ): Map<Path, UniquenessType> =
        node.accept(typeAssigner, inFlow)

}
