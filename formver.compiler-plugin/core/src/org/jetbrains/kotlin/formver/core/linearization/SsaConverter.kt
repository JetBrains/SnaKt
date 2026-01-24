package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.conversion.FreshEntityProducer
import org.jetbrains.kotlin.formver.core.names.SsaVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Exp.TernaryExp
import org.jetbrains.kotlin.formver.viper.ast.Type

class SsaConverter(
    val source: KtSourceElement? = null,
    private var head: UpdatingSsaNode = SsaStartNode()
) {
    private val ssaNameProducer: MutableMap<SymbolicName, FreshEntityProducer<SsaVariableName, SymbolicName>> =
        mutableMapOf()
    private val ssaAssignments: MutableList<Pair<SsaVariableName, Exp>> = mutableListOf()
    private val returnExpressions: MutableList<Pair<Exp, Exp>> = mutableListOf()

    fun branch(
        condition: Exp,
        thenBlock: () -> Unit,
        elseBlock: () -> Unit
    ) {
        val splitPoint = head

        fun runPath(pathCondition: Exp, block: () -> Unit): UpdatingSsaNode {
            val branchNode = splitPoint.generateBranchNodeFromThisNode(pathCondition)
            head = SsaBlockNode(branchNode, branchNode)
            block()
            return head
        }

        val thenResultHead = runPath(condition, thenBlock)
        val elseResultHead = runPath(Exp.Not(condition), elseBlock)
        val joinNode = SsaJoinNode(thenResultHead, elseResultHead, this, splitPoint.branch)

        head = SsaBlockNode(joinNode, joinNode.branch)
    }

    fun foldAssignmentsAndReturnsIntoExpression(): Exp {
        if (returnExpressions.isEmpty()) throw SnaktInternalException(
            source,
            "No return expression was found for translation"
        )
        val defaultBody = returnExpressions.last().second
        val bodyExp = returnExpressions.dropLast(1).foldRight(defaultBody) { expPair, elseBranch ->
            TernaryExp(
                expPair.first,
                expPair.second,
                elseBranch
            )
        }
        return ssaAssignments.foldRight(bodyExp) { assignment, innerScope ->
            Exp.LetBinding(Declaration.LocalVarDecl(assignment.first, Type.Ref), assignment.second, innerScope)
        }
    }

    fun addAssignment(name: SymbolicName, varExp: Exp): SsaVariableName {
        ssaNameProducer.putIfAbsent(name, FreshEntityProducer(::SsaVariableName))
        val ssaName = ssaNameProducer[name]!!.getFresh(name)
        head.updateLatestName(name, ssaName)
        ssaAssignments.add(ssaName to varExp)
        return ssaName
    }

    fun addPhiAssignment(condition: Exp, left: SymbolicName, right: SymbolicName): SsaVariableName {
        if (left !is SsaVariableName || right !is SsaVariableName) {
            throw SnaktInternalException(
                source,
                "Phi Assignments may only be created for SSA variables"
            )
        }
        if (left.baseName != right.baseName) {
            throw SnaktInternalException(
                source,
                "Phi Assignments may only be created for SSA variables referring to the same source variable"
            )
        }
        return addAssignment(
            left.baseName, TernaryExp(
                condition,
                Exp.LocalVar(left, Type.Ref),
                Exp.LocalVar(right, Type.Ref)
            )
        )
    }

    fun addReturn(returnExp: Exp) {
        returnExpressions.add(head.fullBranchingCondition() to returnExp)
    }

    fun resolveVariableName(name: SymbolicName): SymbolicName {
        return head.resolveVariableName(name)
    }
}