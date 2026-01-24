package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.conversion.FreshEntityProducer
import org.jetbrains.kotlin.formver.core.names.SsaVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Exp

/**
 * A node in a SSA-Graph
 */
sealed interface SsaNode {
    // The full condition to be true for control-flow to reach this node
    val fullBranchingCondition: Exp

    // The partial condition to be true for control-flow to reach this node from a previous branch
    val mostRecentBranchingCondition: Exp

    // Shared between nodes: Produces new ssa names
    val ssaNameProducer: MutableMap<SymbolicName, FreshEntityProducer<SsaVariableName, SymbolicName>>

    /**
     * Function used to resolve names to their SSAVariableName
     * Fallsback to provided name if no such name is found
     */
    fun resolveVariableName(name: SymbolicName): SymbolicName

    fun generateBranchingBlockNodeFromThisNode(condition: Exp): SsaBlockNode =
        SsaBlockNode(
            this,
            condition,
            if (fullBranchingCondition == Exp.BoolLit(true)) condition else Exp.And(condition, fullBranchingCondition),
        )

    fun generateFreshSsaName(name: SymbolicName): SsaVariableName {
        ssaNameProducer.putIfAbsent(name, FreshEntityProducer(::SsaVariableName))
        val ssaName = ssaNameProducer[name]!!.getFresh(name)
        return ssaName
    }
}

class SsaStartNode(override val ssaNameProducer: MutableMap<SymbolicName, FreshEntityProducer<SsaVariableName, SymbolicName>> = mutableMapOf()) :
    SsaNode {
    override val mostRecentBranchingCondition: Exp = Exp.BoolLit(true)
    override val fullBranchingCondition: Exp = Exp.BoolLit(true)
    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        name
}

class SsaBlockNode(
    private val predecessor: SsaNode,
    override val mostRecentBranchingCondition: Exp,
    override val fullBranchingCondition: Exp
) : SsaNode {

    override val ssaNameProducer = predecessor.ssaNameProducer
    val latestName: MutableMap<SymbolicName, SsaVariableName> = mutableMapOf()

    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        latestName[name] ?: predecessor.resolveVariableName(name)

    fun registerAndUpdateLatestNameUsage(name: SymbolicName): SsaVariableName {
        val ssaName = generateFreshSsaName(name);
        latestName[name] = ssaName;
        return ssaName
    }
}

class SsaJoinNode(
    private val leftPredecessor: SsaNode,
    private val rightPredecessor: SsaNode,
    private val ssaConverter: SsaConverter,
    override val mostRecentBranchingCondition: Exp,
    override val fullBranchingCondition: Exp,
) : SsaNode {
    private val lookupCache: MutableMap<SymbolicName, SymbolicName> = mutableMapOf()

    override val ssaNameProducer: MutableMap<SymbolicName, FreshEntityProducer<SsaVariableName, SymbolicName>>
        get() {
            if (leftPredecessor.ssaNameProducer != rightPredecessor.ssaNameProducer) throw SnaktInternalException(
                ssaConverter.source,
                "Invalid SSA-Graph. It is expected that all nodes in a graph share the same name producer"
            )
            return leftPredecessor.ssaNameProducer
        }

    override fun resolveVariableName(name: SymbolicName): SymbolicName =
        lookupCache[name] ?: resolveNameFromPredecessors(name)

    private fun resolveNameFromPredecessors(name: SymbolicName): SymbolicName {
        val leftIncoming = leftPredecessor.resolveVariableName(name)
        val rightIncoming = rightPredecessor.resolveVariableName(name)
        return if (rightIncoming == leftIncoming) {
            leftIncoming
        } else if (leftIncoming is SsaVariableName && rightIncoming is SsaVariableName) {
            val ssaName = generateFreshSsaName(leftIncoming.baseName)
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