/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.uniqueness

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

abstract class PathTrie<T, Self : PathTrie<T, Self>>(
    var element: T,
    val children: Map<FirBasedSymbol<*>, Self> = emptyMap(),
) {
    abstract fun T.join(other: T): T

    abstract fun construct(element: T, children: Map<FirBasedSymbol<*>, Self>): Self

    operator fun get(symbol: FirBasedSymbol<*>): Self? =
        children[symbol]

    override fun equals(other: Any?): Boolean {
        return other is PathTrie<*, *> &&
                children == other.children &&
                element == other.element
    }

    override fun hashCode(): Int {
        var result = element.hashCode()
        result = 31 * result + children.hashCode()

        return result
    }
}

fun <T, Self : PathTrie<T, Self>> Self.copy(
    element: T = this.element,
    children: Map<FirBasedSymbol<*>, Self> = this.children
): Self {
    return construct(element, children)
}

fun <T, Self : PathTrie<T, Self>> Self.ensure(
    path: Path,
    initialize: (FirBasedSymbol<*>) -> T
): Self {
    return ensure(path.iterator(), initialize)
}

private fun <T, Self : PathTrie<T, Self>> Self.ensure(
    path: Iterator<FirBasedSymbol<*>>,
    initialize: (FirBasedSymbol<*>) -> T
): Self {
    if (!path.hasNext()) {
        return this
    }

    val symbol = path.next()
    val next = children[symbol] ?: construct(initialize(symbol), emptyMap())
    val newChildren = children + (symbol to next.ensure(path, initialize))

    return construct(element, newChildren)
}

fun <T, Self : PathTrie<T, Self>> Self.join(other: Self): Self {
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
