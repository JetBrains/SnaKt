/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.rendering.Renderer

sealed interface LocalityContract {
    data object Undefined : LocalityContract

    data class FunctionContract(
        val requirements: List<LocalityRequirement>
    ) : LocalityContract
}

fun LocalityContract.join(other: LocalityContract): LocalityContract =
    when {
        this is LocalityContract.Undefined -> other
        other is LocalityContract.Undefined -> this
        this is LocalityContract.FunctionContract && other is LocalityContract.FunctionContract ->
            if (requirements.size == other.requirements.size) {
                LocalityContract.FunctionContract(
                    requirements.zip(other.requirements).map { (thisRequirement, otherRequirement) ->
                        thisRequirement.join(otherRequirement)
                    }
                )
            } else {
                LocalityContract.Undefined
            }
        else -> LocalityContract.Undefined
    }

val LocalityContractRenderer = Renderer<LocalityContract> { contract ->
    when (contract) {
        is LocalityContract.FunctionContract ->
            contract.requirements.joinToString(
                prefix = "'(",
                separator = ", ",
                postfix = ")'",
            ) { requirement ->
                when (requirement) {
                    LocalityRequirement.RequireGlobal -> "global"
                    LocalityRequirement.RequireLocal -> "local"
                }
            }
        is LocalityContract.Undefined -> "undefined"
    }
}
