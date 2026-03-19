package org.jetbrains.kotlin.formver.uniqueness.utils

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.formver.uniqueness.FlowFacts
import org.jetbrains.kotlin.formver.uniqueness.UniquenessTrie


/**
 * This method creates the uniqueness-related content for each CFG Node. To not overload the graph with data, not everything is displayed.
 * In these cases some flow information is added:
 * - The flow changed when executing the node: In and Out flow are displayed
 * - On the border of the function the flow is displayed
 * - On nodes that join multiple flows the flow is displayed
 */
fun render(node: CFGNode<*>, facts: FlowFacts<UniquenessTrie>): String {
    val flowBefore = facts.flowBefore(node)
    val flowAfter = facts.flowAfter(node)
    return buildString {
        when {
            // flows differ
            flowBefore != flowAfter -> {
                appendLine("Before:")
                appendLine(flowBefore.render())
                appendLine()
                appendLine("After:")
                appendLine(flowAfter.render())
            }
            // function boundary start
            node is FunctionEnterNode -> {
                appendLine("Initial:")
                appendLine(flowBefore.render())
            }
            // function boundary end
            node is FunctionExitNode -> {
                appendLine("Final:")
                appendLine(flowAfter.render())
            }
            // joining node
            node.previousNodes.count() > 1 -> {
                appendLine("Merge:")
                appendLine(flowAfter.render())
            }
        }
    }
}

@Suppress("unused") // Can be used from the debugger
fun ControlFlowGraph.render(facts: FlowFacts<UniquenessTrie>): String {
    val options = ControlFlowGraphRenderOptions(
        data = { data: CFGNode<*> -> render(data, facts) },
    )
    return buildString {
        renderTo(this, options)
    }
}
