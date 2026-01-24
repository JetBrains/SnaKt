package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.names.SsaVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Exp.Companion.toConjunction

sealed interface SsaNode {
    val branch: SsaBranchNode?

    fun fullBranchingCondition(): Exp
    fun mostRecentBranchingCondition(): Exp
    fun resolveVariableName(name: SymbolicName): SymbolicName

    fun generateBranchNodeFromThisNode(condition: Exp): SsaBranchNode =
        SsaBranchNode(this, condition, branch)
}

sealed interface UpdatingSsaNode : SsaNode {
    val latestName: MutableMap<SymbolicName, SsaVariableName>

    fun updateLatestName(name: SymbolicName, ssaName: SsaVariableName) {
        latestName[name] = ssaName
    }
}

sealed interface NonBranchingSsaNode : SsaNode {
    override fun mostRecentBranchingCondition(): Exp = branch?.mostRecentBranchingCondition() ?: Exp.BoolLit(true)
    override fun fullBranchingCondition(): Exp = branch?.fullBranchingCondition() ?: Exp.BoolLit(true)
}

class SsaStartNode() : UpdatingSsaNode, NonBranchingSsaNode {
    override val latestName: MutableMap<SymbolicName, SsaVariableName> = mutableMapOf()
    override val branch: SsaBranchNode? = null
    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        latestName[name] ?: name
}

class SsaBlockNode(private val predecessor: SsaNode, override val branch: SsaBranchNode?) : UpdatingSsaNode,
    NonBranchingSsaNode {
    override val latestName: MutableMap<SymbolicName, SsaVariableName> = mutableMapOf()

    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        latestName[name] ?: predecessor.resolveVariableName(name)
}

class SsaBranchNode(private val predecessor: SsaNode, val condition: Exp, override val branch: SsaBranchNode?) :
    SsaNode {
    override fun fullBranchingCondition(): Exp = when (branch) {
        null -> condition
        else -> listOf(branch.fullBranchingCondition(), condition).toConjunction()
    }

    override fun mostRecentBranchingCondition(): Exp = condition

    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        predecessor.resolveVariableName(name)
}

class SsaJoinNode(
    private val leftPredecessor: SsaNode,
    private val rightPredecessor: SsaNode,
    private val ssaConverter: SsaConverter,
    override val branch: SsaBranchNode?
) : SsaNode, NonBranchingSsaNode {
    private val lookupCache: MutableMap<SymbolicName, SymbolicName> = mutableMapOf();

    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        lookupCache[name] ?: resolveNameFromPredecessors(name)

    private fun resolveNameFromPredecessors(name: SymbolicName): SymbolicName {
        val leftIncoming = leftPredecessor.resolveVariableName(name)
        val rightIncoming = rightPredecessor.resolveVariableName(name)
        val resultName = when (leftIncoming) {
            rightIncoming -> leftIncoming // Incoming versions equal each other
            else -> ssaConverter.addPhiAssignment( // Resolve to phi assignment
                leftPredecessor.mostRecentBranchingCondition(),
                leftIncoming,
                rightIncoming
            )
        }
        lookupCache[name] = resultName
        return resultName
    }
}