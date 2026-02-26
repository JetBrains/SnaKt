/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph

/**
 * Stores the flow facts before and after each CFG node.
 *
 * @param F the type of the flow object.
 */
class FlowFacts<F>(
    private val flowBefore: Map<CFGNode<*>, F>,
    private val flowAfter: Map<CFGNode<*>, F>,
) {
    /**
     * Returns the dataflow fact holding *before* the given [node] is executed.
     */
    fun flowBefore(node: CFGNode<*>): F =
        flowBefore[node] ?: error("No flow-before information for node: $node")

    /**
     * Returns the dataflow fact holding *after* the given [node] is executed.
     */
    fun flowAfter(node: CFGNode<*>): F =
        flowAfter[node] ?: error("No flow-after information for node: $node")
}

/**
 * Directions for dataflow analysis.
 */
enum class FlowDirection {
    FORWARD,
    BACKWARD
}

/**
 * A generic, worklist-based dataflow analysis over [ControlFlowGraph].
 *
 * @param F the type of the flow fact.
 * @param direction whether the analysis propagates forward or backward along
 *                  the CFG edges.
 */
abstract class DataFlowAnalyzer<F>(
    val direction: FlowDirection = FlowDirection.FORWARD,
) {

    /**
     * Returns the initial dataflow fact for the entry point of the analysis.
     */
    abstract val initial: F

    /**
     * Returns the initial flow fact for an intermediate point of the analysis.
     */
    abstract val bottom: F

    /**
     * Merges two flow facts at a join point.
     *
     * @param a the first flow fact.
     * @param b the second flow fact.
     * @return the joined flow fact.
     */
    abstract fun join(a: F, b: F): F

    /**
     * The transfer function.  Given the dataflow fact [inFlow] arriving at * [node], returns the fact that holds after
     * [node].
     *
     * @param node the CFG node to transfer.
     * @param inFlow the incoming dataflow fact.
     * @return the outgoing dataflow fact.
     */
    abstract fun transfer(node: CFGNode<*>, inFlow: F): F

    /**
     * Runs the worklist algorithm on [graph] and returns a [FlowFacts] containing the before/after facts for every
     * node.
     *
     * @param graph the control flow graph to analyze.
     * @return a [FlowFacts] containing the before/after facts for every node.
     */
    fun analyze(graph: ControlFlowGraph): FlowFacts<F> {
        val nodes: List<CFGNode<*>> = graph.nodes
        val inFlow = mutableMapOf<CFGNode<*>, F>()
        val outFlow = mutableMapOf<CFGNode<*>, F>()
        val entryNode: CFGNode<*>
        val predecessorsOf: (CFGNode<*>) -> List<CFGNode<*>>

        when (direction) {
            FlowDirection.FORWARD -> {
                entryNode = nodes.first()
                predecessorsOf = { node -> node.previousNodes }
            }
            FlowDirection.BACKWARD -> {
                entryNode = nodes.last()
                predecessorsOf = { node -> node.followingNodes }
            }
        }

        for (node in nodes) {
            inFlow[node] = bottom
            outFlow[node] = bottom
        }

        inFlow[entryNode] = initial
        outFlow[entryNode] = transfer(entryNode, initial)

        val worklist = ArrayDeque<CFGNode<*>>()
        val justAdded = mutableSetOf<CFGNode<*>>()

        for (node in nodes) {
            if (node !== entryNode) {
                worklist.addLast(node)
                justAdded.add(node)
            }
        }

        while (worklist.isNotEmpty()) {
            val node = worklist.removeFirst()
            justAdded.remove(node)
            val predecessors = predecessorsOf(node)

            val joinedIn = if (predecessors.isEmpty()) {
                bottom
            } else {
                predecessors
                    .map { outFlow[it] ?: bottom }
                    .reduce { result, flow -> join(result, flow) }
            }

            inFlow[node] = joinedIn
            val newOut = transfer(node, joinedIn)

            if (newOut != outFlow[node]) {
                outFlow[node] = newOut

                val successors = when (direction) {
                    FlowDirection.FORWARD -> node.followingNodes
                    FlowDirection.BACKWARD -> node.previousNodes
                }

                for (successor in successors) {
                    if (successor !in justAdded) {
                        worklist.addLast(successor)
                        justAdded.add(successor)
                    }
                }
            }
        }

        return when (direction) {
            FlowDirection.FORWARD -> FlowFacts(
                flowBefore = inFlow.toMap(),
                flowAfter = outFlow.toMap(),
            )
            FlowDirection.BACKWARD -> FlowFacts(
                flowBefore = outFlow.toMap(),
                flowAfter = inFlow.toMap(),
            )
        }
    }
}
