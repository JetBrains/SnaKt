package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.names.SsaVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Exp

/**
 * A node in a SSA-Graph
 */
sealed interface SsaNode {

    /**
     * Function used to resolve names to their SSAVariableName
     * Fallsback to provided name if no such name is found
     */
    fun resolveVariableName(name: SymbolicName): SymbolicName
}

class SsaStartNode() :
    SsaNode {
    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        name
}

class SsaBlockNode(
    private val predecessor: SsaNode,
    val mostRecentBranchingCondition: Exp,
    val fullBranchingCondition: Exp
) : SsaNode {

    val latestName: MutableMap<SymbolicName, SsaVariableName> = mutableMapOf()

    fun generateBranchingBlockNodeFromThisNode(condition: Exp): SsaBlockNode =
        SsaBlockNode(
            this,
            condition,
            if (fullBranchingCondition == Exp.BoolLit(true)) condition else Exp.And(condition, fullBranchingCondition),
        )

    context(ssaConverter: SsaConverter)
    fun updateLatestNameUsage(name: SymbolicName): SsaVariableName {
        val ssaName = ssaConverter.generateFreshSsaName(name);
        latestName[name] = ssaName;
        return ssaName
    }

    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        latestName[name] ?: predecessor.resolveVariableName(name)
}

class SsaJoinNode(
    private val leftPredecessor: SsaBlockNode,
    private val rightPredecessor: SsaBlockNode,
    private val ssaConverter: SsaConverter
) : SsaNode {
    private val lookupCache: MutableMap<SymbolicName, SymbolicName> = mutableMapOf()

    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        lookupCache[name] ?: resolveNameFromPredecessors(name)

    private fun resolveNameFromPredecessors(name: SymbolicName): SymbolicName {
        val leftIncoming = leftPredecessor.resolveVariableName(name)
        val rightIncoming = rightPredecessor.resolveVariableName(name)
        return if (rightIncoming == leftIncoming) {
            leftIncoming
        } else if (leftIncoming is SsaVariableName && rightIncoming is SsaVariableName) {
            val ssaName = ssaConverter.generateFreshSsaName(leftIncoming.baseName)
            ssaConverter.addPhiAssignment( // Resolve to phi assignment
                leftPredecessor.mostRecentBranchingCondition,
                leftIncoming,
                rightIncoming,
                ssaName
            )
            ssaName
        } else {
            throw SnaktInternalException(
                ssaConverter.source,
                "Phi Assignments may only be created for SSA variables"
            )
        }
    }
}