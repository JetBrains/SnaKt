/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.uniqueness

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

class UniquenessTrie(
    element: Uniqueness,
    children: Map<FirBasedSymbol<*>, UniquenessTrie> = emptyMap(),
) : PathTrie<Uniqueness, UniquenessTrie>(element, children) {
    override fun Uniqueness.join(other: Uniqueness): Uniqueness =
        this.join(other)

    override fun construct(
        element: Uniqueness,
        children: Map<FirBasedSymbol<*>, UniquenessTrie>
    ): UniquenessTrie {
        return UniquenessTrie(element, children)
    }
}