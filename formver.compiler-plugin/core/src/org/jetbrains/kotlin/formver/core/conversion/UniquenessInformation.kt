/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EnterNodeMarker
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ExitNodeMarker
import org.jetbrains.kotlin.formver.uniqueness.FlowFacts
import org.jetbrains.kotlin.formver.uniqueness.UniquenessTrie


class UniquenessInformation(val root: CFGNode<*>, val flowFacts: FlowFacts<UniquenessTrie>) {

    private val nodeCollectionMap by lazy { extract() }

    fun flowBefore(firElement: FirElement): UniquenessTrie? {
        return nodeCollectionMap[firElement]?.entry?.let { flowFacts.flowBefore(it) }
    }

    fun flowAfter(firElement: FirElement): UniquenessTrie? {
        return nodeCollectionMap[firElement]?.exit?.let { flowFacts.flowAfter(it) }
    }

    class NodeCollection {
        private var _entry: CFGNode<*>? = null
        private var _exit: CFGNode<*>? = null
        private val _all: MutableList<CFGNode<*>> = mutableListOf()


        val entry: CFGNode<*>? get() = _entry ?: _all.firstOrNull()
        val exit: CFGNode<*>? get() = _exit ?: _all.lastOrNull()

        fun update(node: CFGNode<*>) {
            when (node) {
                is EnterNodeMarker -> _entry = node
                is ExitNodeMarker -> _exit = node
                else -> {}
            }
            _all.add(node)
        }
    }

    fun extract(): Map<FirElement, NodeCollection> {
        val visited = mutableSetOf<CFGNode<*>>()
        val result = mutableMapOf<FirElement, NodeCollection>()

        val stack = mutableListOf(root)

        while (stack.isNotEmpty()) {
            val node = stack.removeAt(stack.size - 1)
            if (!visited.add(node)) continue

            result.getOrPut(node.fir) { NodeCollection() }.update(node)

            for (followingNode in node.followingNodes) {
                stack.add(followingNode)
            }
        }

        return result
    }
}

