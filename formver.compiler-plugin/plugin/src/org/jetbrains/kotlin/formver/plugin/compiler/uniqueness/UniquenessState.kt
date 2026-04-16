/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.uniqueness

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.PathTrie

class UniquenessState(
    element: Uniqueness,
    children: Map<FirBasedSymbol<*>, UniquenessState> = emptyMap(),
) : PathTrie<Uniqueness, UniquenessState>(element, children) {
    override fun Uniqueness.join(other: Uniqueness): Uniqueness =
        this.join(other)

    override fun construct(
        element: Uniqueness,
        children: Map<FirBasedSymbol<*>, UniquenessState>
    ): UniquenessState {
        return UniquenessState(element, children)
    }
}