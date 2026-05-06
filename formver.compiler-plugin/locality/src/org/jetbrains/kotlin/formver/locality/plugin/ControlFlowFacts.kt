/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.fir.analysis.cfa.util.merge
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EdgeLabel

typealias ControlFlowFacts<K, V> = PersistentMap<K, V>

typealias PathAwareControlFlowFacts<K, V> = PersistentMap<EdgeLabel, ControlFlowFacts<K, V>>

fun <K, V> PathAwareControlFlowFacts<K, V>.collapse(merge: (V, V) -> V): ControlFlowFacts<K, V> =
    values.fold(persistentMapOf()) { result, facts ->
        result.merge(facts, merge)
    }
