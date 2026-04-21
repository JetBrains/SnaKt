/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EnterNodeMarker
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ExitNodeMarker
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularPropertySymbol
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.FoldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.PrimitiveFieldAccess
import org.jetbrains.kotlin.formver.core.embeddings.expression.UnfoldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.properties.BackingFieldGetter
import org.jetbrains.kotlin.formver.uniqueness.*

data class Folds(val unfolds: List<Path>, val folds: List<Path>) {

    private fun createExpEmbeddings(data: StmtConversionContext, path: Path): ExpEmbedding {
        val base = data.embedLocalSymbol(path.first())
        return path.drop(1).fold(base) { path, symbol ->
            val field = (data.embedProperty(symbol as FirRegularPropertySymbol).getter as BackingFieldGetter).field
            PrimitiveFieldAccess(path, field)
        }
    }

    fun unfolds(data: StmtConversionContext) = unfolds.map {
        UnfoldEmbedding(createExpEmbeddings(data, it))
    }

    fun folds(data: StmtConversionContext) = folds.map {
        FoldEmbedding(createExpEmbeddings(data, it))
    }
}

sealed interface Node {
    data class Symbol(val symbol: FirBasedSymbol<*>) : Node {
        override fun holds(other: FirBasedSymbol<*>): Boolean = symbol == other
    }

    object Root : Node {
        override fun holds(other: FirBasedSymbol<*>): Boolean = false
    }

    fun holds(other: FirBasedSymbol<*>): Boolean
}

data class PathTreeNode(
    val element: Node,
    val children: MutableList<PathTreeNode>,
    val typeBefore: UniquenessType? = null,
    val typeAfter: UniquenessType? = null,
) {

    context(firExpression: FirElement)
    fun insertPath(path: Path, data: UniquenessInformation) {
        if (path.isEmpty()) return
        val first = path.first()
        val childMatches = children.firstOrNull {
            it.element.holds(first)
        }
        if (childMatches != null) {
            childMatches.insertPath(path.drop(1), data)
        } else {
            val subTree = PathTreeNode(
                Node.Symbol(first),
                mutableListOf(),
                data.typeBefore(firExpression, path),
                data.typeAfter(firExpression, path),
            )
            subTree.insertPath(path.drop(1), data)
            children.add(subTree)
        }
    }

    fun allSubPaths(): List<Path> {
        if (element is Node.Root) return emptyList()
        return children.flatMap { it.allSubPaths() }.map { listOf((element as Node.Symbol).symbol) + it }
    }

    fun isMoved(type: UniquenessType): Boolean = type == UniquenessType.Moved

    fun extractFolds(): List<Path> {
        if (element is Node.Root) return children.flatMap { it.extractFolds() }
        val currentSymbol = (element as Node.Symbol).symbol

        if (children.isEmpty()) {
            return emptyList()
        }

        return when (typeAfter) {
            is UniquenessType.Moved -> children.flatMap { it.extractFolds() }.map { listOf(currentSymbol) + it }
            else -> children.flatMap { it.extractFolds() }.map { listOf(currentSymbol) + it } + listOf(
                listOf(
                    currentSymbol
                )
            )
        }
    }

    fun extractUnfolds(): List<Path> {
        if (element is Node.Root) return children.flatMap { it.extractUnfolds() }
        val currentSymbol = (element as Node.Symbol).symbol

        if (children.isEmpty()) {
            return emptyList()
        }

        return when (typeBefore) {
            is UniquenessType.Moved -> children.flatMap { it.extractUnfolds() }.map { listOf(currentSymbol) + it }
            // Before Unique
            else -> listOf(listOf(currentSymbol)) + children.flatMap { it.extractUnfolds() }
                .map { listOf(currentSymbol) + it }
        }
    }
}

/**
 * Represents information about the uniqueness of paths through a control flow graph (CFG).
 * It provides mechanisms to access dataflow facts and analyze paths before and after a specific
 * FirElement
 *
 * @property root The root node of the control flow graph (CFG) from which traversal begins.
 * @property flowFacts The dataflow facts associated with each node in the CFG, which store
 *                     information before and after execution of those nodes.
 */
class UniquenessInformation(val root: CFGNode<*>, val flowFacts: FlowFacts<UniquenessTrie>) {

    private val nodeCollectionMap by lazy { extract() }

    fun flowBefore(firElement: FirElement): UniquenessTrie? {
        return nodeCollectionMap[firElement]?.entry?.let { flowFacts.flowBefore(it) }
    }

    fun flowAfter(firElement: FirElement): UniquenessTrie? {
        return nodeCollectionMap[firElement]?.exit?.let { flowFacts.flowAfter(it) }
    }

    fun typeBefore(firElement: FirElement, path: Path): UniquenessType? {
        return flowBefore(firElement)?.ensure(path)?.parentsJoin
    }

    fun typeAfter(firElement: FirElement, path: Path): UniquenessType? {
        return flowAfter(firElement)?.ensure(path)?.parentsJoin
    }


    fun getFolds(embedding: FirElement): Folds {

        val pathTree = PathTreeNode(Node.Root, mutableListOf())
        with(embedding) {
            embedding.valuePaths.forEach { path ->
                pathTree.insertPath(path, this@UniquenessInformation)
            }
        }

        return Folds(pathTree.extractUnfolds(), pathTree.extractFolds())
    }


    class NodeCollection {
        private var _entry: CFGNode<*>? = null
        private var _exit: CFGNode<*>? = null
        private val all: MutableList<CFGNode<*>> = mutableListOf()


        val entry: CFGNode<*>? get() = _entry ?: all.firstOrNull()
        val exit: CFGNode<*>? get() = _exit ?: all.lastOrNull()

        fun update(node: CFGNode<*>) {
            when (node) {
                is EnterNodeMarker -> _entry = node
                is ExitNodeMarker -> _exit = node
                else -> {}
            }
            all.add(node)
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

