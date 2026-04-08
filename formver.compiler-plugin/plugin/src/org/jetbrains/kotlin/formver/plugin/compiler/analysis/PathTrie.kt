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

    abstract fun initialize(symbol: FirBasedSymbol<*>): T

    abstract fun construct(element: T, children: Map<FirBasedSymbol<*>, PathTrie<T>>): PathTrie<T>

    fun copy(element: T = this.element, children: Map<FirBasedSymbol<*>, PathTrie<T>> = this.children): PathTrie<T> {
        return construct(element, children)
    }

    fun ensure(path: Path): PathTrie<T> {
        if (path.isEmpty()) {
            return this
        }

        val symbol = path.first()
        val rest = path.drop(1)
        val next = children[symbol]

        val newChildren = if (next != null) {
            children + (symbol to next.ensure(rest))
        } else {
            children + (symbol to construct(initialize(symbol), mapOf()))
        }

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
