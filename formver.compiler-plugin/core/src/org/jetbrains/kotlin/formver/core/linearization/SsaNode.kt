package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.formver.core.names.SsaVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Exp.Companion.toConjunction
import org.jetbrains.kotlin.formver.viper.ast.Type

abstract class SsaNode(val branches: List<SsaBranchNode>) {
    protected val latestName: MutableMap<SymbolicName, SsaVariableName> = mutableMapOf()

    fun fullBranchingCondition(): Exp = branches.map { it.condition }.toConjunction()
    fun mostRecentBranchingCondition(): Exp = branches.lastOrNull()?.condition ?: Exp.BoolLit(true)
    fun updateLatestName(name: SymbolicName, ssaName: SsaVariableName) {
        latestName[name] = ssaName
    }

    fun generateBranchNodeFromThisNode(condition: Exp): SsaBranchNode {
        val copiedBranches = branches.toMutableList()
        return SsaBranchNode(this, condition, copiedBranches).also {
            copiedBranches.add(it)
        }
    }

    abstract fun resolveVariableName(name: SymbolicName): SymbolicName
}

class SsaStartNode() : SsaNode(listOf()) {
    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        latestName[name] ?: name
}

class SsaBlockNode(private val predecessor: SsaNode, branches: List<SsaBranchNode>) : SsaNode(branches) {
    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        latestName[name] ?: predecessor.resolveVariableName(name)
}

class SsaBranchNode(private val predecessor: SsaNode, val condition: Exp, branches: List<SsaBranchNode>) :
    SsaNode(branches) {
    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        predecessor.resolveVariableName(name)
}

class SsaJoinNode(
    private val leftPredecessor: SsaNode,
    private val rightPredecessor: SsaNode,
    private val ssaConverter: SsaConverter,
    branches: List<SsaBranchNode>
) : SsaNode(branches) {
    private val lookupCache: MutableMap<SymbolicName, SymbolicName> = mutableMapOf();

    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        lookupCache[name] ?: resolveNameFromPredecessors(name)

    private fun resolveNameFromPredecessors(name: SymbolicName): SymbolicName {
        val leftIncoming = leftPredecessor.resolveVariableName(name)
        val rightIncoming = rightPredecessor.resolveVariableName(name)
        val resultName = when (leftIncoming) {
            rightIncoming -> leftIncoming
            is SsaVariableName -> ssaConverter.addAssignment(
                leftIncoming.baseName, Exp.TernaryExp(
                    leftPredecessor.mostRecentBranchingCondition(),
                    Exp.LocalVar(leftIncoming, Type.Ref),
                    Exp.LocalVar(rightIncoming, Type.Ref)
                )
            )

            else -> name
        }
        lookupCache[name] = resultName
        return resultName
    }
}