package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.names.SsaVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Exp.TernaryExp
import org.jetbrains.kotlin.formver.viper.ast.Type

class SsaConverter(
    val source: KtSourceElement? = null,
    var head: SsaNode = SsaStartNode()
) {
    private val ssaAssignments: MutableList<Pair<SsaVariableName, Exp>> = mutableListOf()
    private val returnExpressions: MutableList<Pair<Exp, Exp>> = mutableListOf()
    private val variableIndex: MutableMap<SymbolicName, Int> = mutableMapOf()

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
        val ssaIdx = (variableIndex[name]?.plus(1) ?: 0)
        val ssaName = SsaVariableName(name, ssaIdx)
        head.updateLatestName(name, ssaName)
        ssaAssignments.add(ssaName to varExp)
        variableIndex[name] = ssaIdx
        return ssaName
    }

    fun addReturn(returnExp: Exp) {
        returnExpressions.add(head.fullBranchingCondition() to returnExp)
    }
    
    fun resolveVariableName(name: SymbolicName): SymbolicName {
        return head.resolveVariableName(name)
    }
}