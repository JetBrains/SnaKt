/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.formver.type.plugin.TypeIntersector
import org.jetbrains.kotlin.formver.type.plugin.TypeUnifier

data class PathTrie<Type>(
    val data: Type,
    val children: PersistentMap<FirBasedSymbol<*>, PathTrie<Type>> = persistentMapOf(),
)

fun <Type> PathTrie<Type>.associate(symbol: FirBasedSymbol<*>, child: PathTrie<Type>): PathTrie<Type> =
    copy(children = children.put(symbol, child))

val PathTrie<*>.symbols: Sequence<FirBasedSymbol<*>>
    get() = children.keys.asSequence() + children.values.flatMap { it.symbols }

fun <Type> PathTrie<Type>.append(symbol: FirBasedSymbol<*>, other: PathTrie<Type>): PathTrie<Type> {
    if (children.isEmpty()) {
        return copy(children = children.put(symbol, other))
    }

    var appendedChildren = children

    for ((childSymbol, child) in children) {
        appendedChildren = appendedChildren.put(childSymbol, child.append(symbol, other))
    }

    return copy(children = appendedChildren)
}

fun <Type> PathTrie<Type>.join(other: PathTrie<Type>, typeUnifier: TypeUnifier<Type>): PathTrie<Type> {
    var joinedChildren = children

    for ((symbol, otherChild) in other.children) {
        val child = joinedChildren[symbol]

        joinedChildren = joinedChildren.put(
            symbol,
            child?.join(otherChild, typeUnifier) ?: otherChild,
        )
    }

    return copy(
        data = typeUnifier.join(data, other.data),
        children = joinedChildren
    )
}

fun <Type> PathTrie<Type>.meet(other: PathTrie<Type>, typeIntersector: TypeIntersector<Type>): PathTrie<Type> {
    var metChildren = persistentMapOf<FirBasedSymbol<*>, PathTrie<Type>>()

    for ((symbol, child) in children) {
        val otherChild = other.children[symbol] ?: continue
        metChildren = metChildren.put(symbol, child.meet(otherChild, typeIntersector))
    }

    return copy(
        data = typeIntersector.meet(data, other.data),
        children = metChildren
    )
}

fun <Type> PathTrie<Type>.joinChildren(typeUnifier: TypeUnifier<Type>): Type {
    var joinedData = data

    for (child in children.values) {
        joinedData = typeUnifier.join(joinedData,child.joinChildren(typeUnifier))
    }

    return joinedData
}
