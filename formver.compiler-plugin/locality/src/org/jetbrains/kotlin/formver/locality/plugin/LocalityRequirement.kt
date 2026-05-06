/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.locality.plugin.Locality.Global
import org.jetbrains.kotlin.formver.locality.plugin.Locality.Local

enum class LocalityRequirement : Constraint<Locality> {
    RequireGlobal, RequireLocal;

    context(context: CheckerContext)
    override fun accepts(value: Locality): Boolean =
        when (this) {
            RequireGlobal -> value == Global
            RequireLocal ->
                when (value) {
                    Global -> true
                    is Local -> value.owner == context.findClosest<FirFunctionSymbol<*>>()
                }
    }

    fun join(other: LocalityRequirement): LocalityRequirement =
        minOf(this, other)
}

fun List<LocalityRequirement>.accepts(value: List<LocalityRequirement>): Boolean =
    size == value.size &&
            zip(value).all { (thisRequirement, otherRequirement) ->
                thisRequirement <= otherRequirement
            }
