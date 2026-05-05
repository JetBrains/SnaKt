/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.locality.plugin.Locality.Global
import org.jetbrains.kotlin.formver.locality.plugin.Locality.Local
import org.jetbrains.kotlin.formver.locality.plugin.LocalityRequirement.RequireGlobal
import org.jetbrains.kotlin.formver.locality.plugin.LocalityRequirement.RequireLocal

object LocalityWitnessGenerator : WitnessGenerator<Locality, LocalityRequirement> {
    context(context: CheckerContext)
    override fun generateWitnessFor(requirement: LocalityRequirement): Locality =
        when (requirement) {
            RequireGlobal -> Global
            RequireLocal -> Local(context.findClosest<FirFunctionSymbol<*>>())
        }
}

context(_: CheckerContext)
fun LocalityRequirement.generateWitness(): Locality =
    LocalityWitnessGenerator.generateWitnessFor(this)
