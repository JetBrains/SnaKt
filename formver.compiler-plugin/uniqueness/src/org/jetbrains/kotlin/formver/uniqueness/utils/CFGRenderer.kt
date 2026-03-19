package org.jetbrains.kotlin.formver.uniqueness.utils

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphRenderOptions
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.renderTo
import org.jetbrains.kotlin.formver.uniqueness.FlowFacts
import org.jetbrains.kotlin.formver.uniqueness.UniquenessTrie


fun render(node: CFGNode<*>, facts: FlowFacts<UniquenessTrie>): String {
    val flowBefore = facts.flowBefore(node)
    val flowAfter = facts.flowAfter(node)
    val builder = StringBuilder()

    builder.appendLine("Before:")
    builder.appendLine(flowBefore.render())
    builder.appendLine("After:")
    builder.appendLine(flowAfter.render())
    return builder.toString()
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
