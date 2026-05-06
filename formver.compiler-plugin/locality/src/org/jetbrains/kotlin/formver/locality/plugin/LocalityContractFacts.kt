/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EdgeLabel

/**
 * Contains locality-contract information for each `FirExpression` in the control flow, normalized through
 * [unwrapExpression].
 */
typealias LocalityContractFacts = ControlFlowFacts<FirExpression, LocalityContract>

/**
 * Contains locality-contract information for every type of execution flow.
 */
typealias PathAwareLocalityContractFacts = ControlFlowFacts<EdgeLabel, LocalityContractFacts>
