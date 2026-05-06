/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext

sealed interface LocalityContract : LatticeElement<LocalityContract>, Constraint<LocalityContract> {
    companion object {
        operator fun invoke(requiredLocalities: List<LocalityRequirement>): LocalityContract =
            ActiveContract(requiredLocalities)
    }

    context(context: CheckerContext)
    override fun accepts(value: LocalityContract): Boolean =
        when (this) {
            EmptyContract -> value == EmptyContract
            is ActiveContract -> value is ActiveContract &&
                    requiredLocalities.size == value.requiredLocalities.size &&
                    requiredLocalities.zip(value.requiredLocalities).all { (thisRequirement, otherRequirement) ->
                        thisRequirement < otherRequirement
                    }
        }

    override fun union(other: LocalityContract): LocalityContract =
        when {
            this == EmptyContract -> other
            other == EmptyContract -> this
            this is ActiveContract && other is ActiveContract -> union(other)
            else -> EmptyContract
        }

    override fun meet(other: LocalityContract): LocalityContract =
        when {
            this == EmptyContract || other == EmptyContract -> EmptyContract
            this is ActiveContract && other is ActiveContract -> meet(other)
            else -> EmptyContract
        }
}

data object EmptyContract : LocalityContract

data class ActiveContract(
    val requiredLocalities: List<LocalityRequirement>
) : LocalityContract {
    fun union(other: ActiveContract): LocalityContract {
        if (requiredLocalities.size != other.requiredLocalities.size) {
            return EmptyContract
        }

        return LocalityContract(
            requiredLocalities.zip(other.requiredLocalities)
                .map { (thisRequirement, otherRequirement) ->
                    thisRequirement.union(otherRequirement)
                }
        )
    }

    fun meet(other: ActiveContract): LocalityContract {
        if (requiredLocalities.size != other.requiredLocalities.size) {
            return EmptyContract
        }

        return LocalityContract(
            requiredLocalities.zip(other.requiredLocalities)
                .map { (thisRequirement, otherRequirement) ->
                    thisRequirement.meet(otherRequirement)
                }
        )
    }
}

/**
 * Renders a locality contract for diagnostics.
 */
val LocalityContractRenderer = Renderer<LocalityContract> { contract ->
    when (contract) {
        EmptyContract -> "'empty'"
        is ActiveContract -> contract.requiredLocalities.joinToString(
            prefix = "'(",
            separator = ", ",
            postfix = ")'",
        ) { requirement ->
            when (requirement) {
                LocalityRequirement.RequireGlobal -> "global"
                LocalityRequirement.RequireLocal -> "local"
            }
        }
    }
}
