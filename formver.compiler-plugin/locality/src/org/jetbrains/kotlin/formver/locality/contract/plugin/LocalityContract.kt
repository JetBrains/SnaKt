/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.contract.plugin

import org.jetbrains.kotlin.formver.locality.plugin.Locality
import org.jetbrains.kotlin.formver.locality.plugin.LocalityIntersector
import org.jetbrains.kotlin.formver.locality.plugin.LocalityJudgment
import org.jetbrains.kotlin.formver.locality.plugin.LocalityRenderer
import org.jetbrains.kotlin.formver.type.contract.plugin.FunctionTypeFactJudgment
import org.jetbrains.kotlin.formver.type.contract.plugin.FunctionTypeFactRenderer
import org.jetbrains.kotlin.formver.type.contract.plugin.FunctionTypeFactUnifier
import org.jetbrains.kotlin.formver.type.contract.plugin.FunctionTypeFact

typealias LocalityContract = FunctionTypeFact<Locality>

val LocalityContractRenderer = FunctionTypeFactRenderer(LocalityRenderer)

val LocalityContractUnifier = FunctionTypeFactUnifier(LocalityIntersector)

val LocalityContractJudgment = FunctionTypeFactJudgment(LocalityJudgment)
