/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.fir.analysis.cfa.util.merge
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EdgeLabel

/**
 * Contains locality information for each `FirExpression` in the control flow, normalized through [unwrapExpression].
 */
typealias LocalityFacts = PersistentMap<FirExpression, Locality>

/**
 * Contains locality information for every type of execution flow.
 */
typealias PathAwareLocalityFacts = PersistentMap<EdgeLabel, LocalityFacts>

/**
 * Merges the locality information for every type of execution flow.
 */
fun PathAwareLocalityFacts.collapse(): LocalityFacts =
    values.fold(persistentMapOf()) { result, info ->
        result.merge(info, Locality::union)
    }
