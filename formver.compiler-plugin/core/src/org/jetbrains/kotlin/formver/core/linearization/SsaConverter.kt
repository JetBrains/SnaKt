package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.names.SsaVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Exp.TernaryExp
import org.jetbrains.kotlin.formver.viper.ast.Type
import org.jetbrains.kotlin.formver.viper.debugMangled

class SsaConverter(
    val source: KtSourceElement? = null,
    val variableIndex: MutableMap<SymbolicName, Int> = mutableMapOf(),
    val predecessors: MutableList<SsaConverter> = mutableListOf(),
    val successors: MutableList<SsaConverter> = mutableListOf(),
    val localBranchingCondition: Exp? = null,
    val globalBranchingCondition: Exp? = null
) {
    val assignments: MutableList<SsaAssignment> = mutableListOf()
    var body: Exp? = null

    fun asExp(): Exp {
        val executionOrder = getTopologicallySortedBlocks()
        val allAssignments = executionOrder.flatMap { it.assignments }
        if (!isBodyDefinedForAnyPath(executionOrder.last())) throw SnaktInternalException(
            source,
            "Not all control-flow paths lead to a return expression."
        )
        val allBodyConverters = executionOrder.filter { it.body != null }
        val defaultBody = allBodyConverters.last().body!!
        val bodyExp = allBodyConverters.dropLast(1).foldRight(defaultBody) { converter, elseBranch ->
            val body: Exp = converter.body!!
            TernaryExp(
                converter.globalBranchingCondition ?: Exp.BoolLit(true),
                body,
                elseBranch
            )
        }
        return allAssignments.foldRight(bodyExp) { (decl, ssaIdx, expr), innerScope ->
            val ssaDecl = decl.copy(name = SsaVariableName(decl.name, ssaIdx))
            Exp.LetBinding(ssaDecl, expr, innerScope)
        }
    }

    /**
     * Checks if a body is defined on all paths upwards.
     */
    private fun isBodyDefinedForAnyPath(ssaConverter: SsaConverter): Boolean {
        return checkUpstream(ssaConverter, mutableSetOf())
    }

    private fun checkUpstream(current: SsaConverter, visited: MutableSet<SsaConverter>): Boolean =
        when {
            current.body != null -> true
            current.predecessors.isEmpty() -> false
            !visited.add(current) -> false
            else -> current.predecessors.all { pred ->
                checkUpstream(pred, visited)
            }
        }

    private fun getTopologicallySortedBlocks(): List<SsaConverter> {
        val visited = mutableSetOf<SsaConverter>()
        val result = mutableListOf<SsaConverter>()
        val queue = ArrayDeque(listOf(this))
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            result.add(current)
            visited.add(current)
            current.successors.forEach { successor ->
                if (successor !in queue && successor !in visited) {
                    val allPredecessorsVisited = successor.predecessors.all { it in visited }
                    if (allPredecessorsVisited) {
                        queue.add(successor)
                    }
                }
            }
        }
        return result
    }

    fun addAssignment(decl: Declaration.LocalVarDecl, varExp: Exp) {
        val ssaIdx = (variableIndex[decl.name]?.plus(1) ?: 0)
        variableIndex[decl.name] = ssaIdx
        assignments.add(SsaAssignment(decl, ssaIdx, varExp))
    }

    fun addBody(body: Exp) {
        this.body = body
    }

    /**
     * Resolves the symbolic name to its most recent SSA definition.
     * If no local assignment is found, we assume the provided name is valid
     */
    fun resolveVariableName(name: SymbolicName): SymbolicName {
        findLocalSsaVersion(name)?.let { return it }
        return when (predecessors.size) {
            1 -> predecessors.first().resolveVariableName(name)
            else -> resolvePhiOrFallback(name)
        }
    }

    private fun resolvePhiOrFallback(name: SymbolicName): SymbolicName {
        val incomingVersions = predecessors.map { converter ->
            converter.localBranchingCondition to converter.resolveVariableName(name)
        }
        val allAreSsa = incomingVersions.all { (_, n) -> n is SsaVariableName }
        val anyAreSsa = incomingVersions.any { (_, n) -> n is SsaVariableName }
        return when {
            incomingVersions.isEmpty() -> name // Fallback if no versions are incoming
            allAreSsa -> createPhiAssignment(name, incomingVersions)
            anyAreSsa -> throw SnaktInternalException(
                source,
                "Inconsistent SSA state for variable ${name.debugMangled}"
            )
            else -> name // Fallback if all SSAConverters agree, that variable is not an SSAAssignment
        }
    }

    private fun createPhiAssignment(
        originalName: SymbolicName,
        incomingVersions: List<Pair<Exp?, SymbolicName>>
    ): SsaVariableName {
        val expressionPairs = incomingVersions.map { (cond, ssaName) ->
            cond to Exp.LocalVar(ssaName, Type.Ref) as Exp
        }
        val phiExpression = expressionPairs
            .reduce { (_, accExp), (nextCond, nextExp) ->
                val condition = nextCond ?: Exp.BoolLit(true)
                nextCond to TernaryExp(condition, nextExp, accExp)
            }.second
        addAssignment(Declaration.LocalVarDecl(originalName, Type.Ref), phiExpression)
        return SsaVariableName(originalName, assignments.last { it.declaration.name == originalName }.ssaIdx)
    }

    private fun findLocalSsaVersion(name: SymbolicName): SsaVariableName? {
        return assignments
            .lastOrNull { it.declaration.name == name }
            ?.let { SsaVariableName(name, it.ssaIdx) }
    }
}

data class SsaAssignment(val declaration: Declaration.LocalVarDecl, val ssaIdx: Int, val exp: Exp)