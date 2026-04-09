/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.analysis

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

abstract class PathTrie<T>(
    var element: T,
    val children: Map<FirBasedSymbol<*>, PathTrie<T>> = emptyMap(),
) {
    abstract fun T.join(other: T): T

    abstract fun construct(element: T, children: Map<FirBasedSymbol<*>, PathTrie<T>>): PathTrie<T>

    val childrenJoin: T
        get() = children.values.fold(element) {
            result, child ->
            result.join(child.childrenJoin)
        }

    fun copy(element: T = this.element, children: Map<FirBasedSymbol<*>, PathTrie<T>> = this.children): PathTrie<T> {
        return construct(element, children)
    }

    fun ensure(path: Path, initialize: (FirBasedSymbol<*>) -> T): PathTrie<T> {
        return ensure(path.iterator(), initialize)
    }

    private fun ensure(path: Iterator<FirBasedSymbol<*>>, initialize: (FirBasedSymbol<*>) -> T): PathTrie<T> {
        if (!path.hasNext()) {
            return this
        }

        val symbol = path.next()
        val next = children[symbol] ?: construct(initialize(symbol), emptyMap())
        val newChildren = children + (symbol to next.ensure(path, initialize))

        return construct(element, newChildren)
    }

    fun join(other: PathTrie<T>): PathTrie<T> {
        val newElement = element.join(other.element)
        val newChildren = (children.keys + other.children.keys).associateWith { key ->
            val left = children[key]
            val right = other.children[key]

            when {
                left != null && right != null -> left.join(right)
                left != null -> left
                else -> right!!
            }
        }

        return construct(newElement, newChildren)
    }

    override fun equals(other: Any?): Boolean {
        return other is PathTrie<T> &&
                children == other.children &&
                element == other.element
    }

    override fun hashCode(): Int {
        var result = element.hashCode()
        result = 31 * result + children.hashCode()

        return result
    }
}
