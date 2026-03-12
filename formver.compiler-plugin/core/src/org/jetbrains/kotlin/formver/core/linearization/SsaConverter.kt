package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.conversion.FreshEntityProducer
import org.jetbrains.kotlin.formver.core.names.SsaVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Type

class SsaConverter(
    val source: KtSourceElement? = null,
) {
    private var head: SsaBlockNode = SsaBlockNode(SsaStartNode(), Exp.BoolLit(true))
    private val ssaAssignments: MutableList<Pair<SsaVariableName, Exp>> = mutableListOf()
    private val returnExpressions: MutableList<Pair<Exp, Exp>> = mutableListOf()
    private val accessInvariants: MutableMap<SsaVariableName, List<Exp.PredicateAccess>> = mutableMapOf()

    // Produce new ssa names for a source variable name
    private val ssaNameProducers: MutableMap<SymbolicName, FreshEntityProducer<SsaVariableName, SymbolicName>> =
        mutableMapOf()

    fun branch(
        condition: Exp,
        thenBlock: () -> Unit,
        elseBlock: () -> Unit
    ) {
        val splitPoint = head
        head = splitPoint.generateBranchingBlockNodeFromThisNode(condition)
        thenBlock()
        val thenResultHead = head
        head = splitPoint.generateBranchingBlockNodeFromThisNode(Exp.Not(condition))
        elseBlock()
        val joinNode = SsaJoinNode(
            thenResultHead,
            head,
            condition,
            this
        )
        head = SsaBlockNode(joinNode, splitPoint.fullBranchingCondition)
    }

    fun constructExpression(): Exp {
        if (returnExpressions.isEmpty()) throw SnaktInternalException(
            source,
            "No return expression was found for translation"
        )
        val defaultBody = returnExpressions.last().second
        val bodyExp = returnExpressions.dropLast(1).foldRight(defaultBody) { expPair, elseBranch ->
            Exp.TernaryExp(
                expPair.first,
                expPair.second,
                elseBranch
            )
        }
        return ssaAssignments.foldRight(bodyExp) { assignment, innerScope ->
            Exp.LetBinding(Declaration.LocalVarDecl(assignment.first, Type.Ref), assignment.second, innerScope)
        }
    }

    fun generateFreshSsaName(name: SymbolicName): SsaVariableName {
        val producer = ssaNameProducers.getOrPut(name) { FreshEntityProducer(::SsaVariableName) }
        return producer.getFresh(name)
    }

    fun addAssignment(
        name: SymbolicName,
        varExp: Exp,
        newVarAccessInvariants: List<Exp.PredicateAccess> = emptyList()
    ) {
        val ssaName = head.updateLatestName(name)
        addGuardedAssignment(ssaName, varExp)
    }

    fun addPhiAssignment(condition: Exp, left: SsaVariableName, right: SsaVariableName, name: SsaVariableName) {
        if (left.baseName != right.baseName) {
            throw SnaktInternalException(
                source,
                "Phi Assignments may only be created for SSA variables referring to the same source variable."
            )
        }
        val selectionTernary = TernaryExp(
            condition,
            Exp.LocalVar(left, Type.Ref),
            Exp.LocalVar(right, Type.Ref)
        )
        addGuardedAssignment(name, selectionTernary)
    }

    fun addReturn(returnExp: Exp) {
        returnExpressions.add(head.fullBranchingCondition to returnExp)
    }

    fun resolveVariableName(name: SymbolicName): SymbolicName {
        return head.resolveVariableName(name)
    }

    private fun addGuardedAssignment(name: SsaVariableName, varExp: Exp) {
        val defaultExpression = varExp.type.defaultExpression() ?: throw SnaktInternalException(
            source,
            "Tried to assign a variable without a default expression"
        )
        if (head.fullBranchingCondition == Exp.BoolLit(true)) {
            ssaAssignments.add(name to varExp)
        } else {
            ssaAssignments.add(name to Exp.TernaryExp(head.fullBranchingCondition, varExp, defaultExpression))
        }
    }
}