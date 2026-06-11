/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.contract.plugin

import org.jetbrains.kotlin.formver.locality.plugin.Locality
import org.jetbrains.kotlin.formver.locality.plugin.LocalityIntersector
import org.jetbrains.kotlin.formver.locality.plugin.LocalityJudgment
import org.jetbrains.kotlin.formver.locality.plugin.LocalityRenderer
import org.jetbrains.kotlin.formver.type.contract.plugin.TypeContractFactJudgment
import org.jetbrains.kotlin.formver.type.contract.plugin.TypeContractFactRenderer
import org.jetbrains.kotlin.formver.type.contract.plugin.TypeContractFactUnifier
import org.jetbrains.kotlin.formver.type.contract.plugin.TypeContractFact

typealias LocalityContract = TypeContractFact<Locality>

val LocalityContractRenderer = TypeContractFactRenderer(LocalityRenderer)

val LocalityContractUnifier = TypeContractFactUnifier(LocalityIntersector)

val LocalityContractJudgment = TypeContractFactJudgment(LocalityJudgment)
