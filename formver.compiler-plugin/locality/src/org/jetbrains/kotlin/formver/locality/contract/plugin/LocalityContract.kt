/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.contract.plugin

import org.jetbrains.kotlin.formver.locality.plugin.Locality
import org.jetbrains.kotlin.formver.locality.plugin.LocalityIntersector
import org.jetbrains.kotlin.formver.locality.plugin.LocalityJudgment
import org.jetbrains.kotlin.formver.locality.plugin.LocalityRenderer
import org.jetbrains.kotlin.formver.type.contract.plugin.TypeContractJudgment
import org.jetbrains.kotlin.formver.type.contract.plugin.TypeContractRenderer
import org.jetbrains.kotlin.formver.type.contract.plugin.TypeContractUnifier
import org.jetbrains.kotlin.formver.type.contract.plugin.TypeContract

typealias LocalityContract = TypeContract<Locality>

val LocalityContractRenderer = TypeContractRenderer(LocalityRenderer)

val LocalityContractUnifier = TypeContractUnifier(LocalityIntersector)

val LocalityContractJudgment = TypeContractJudgment(LocalityJudgment)
