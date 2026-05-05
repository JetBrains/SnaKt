/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext

data class LocalityContract(
    val requiredLocalities: List<LocalityRequirement>
) : AbstractValue<LocalityContract>, Constraint<LocalityContract> {
    context(context: CheckerContext)
    override fun accepts(value: LocalityContract): Boolean =
        requiredLocalities.size == value.requiredLocalities.size &&
                requiredLocalities.zip(value.requiredLocalities).all { (thisRequirement, otherRequirement) ->
                    thisRequirement < otherRequirement
                }

    override fun union(other: LocalityContract): LocalityContract {
        if (requiredLocalities.size != other.requiredLocalities.size) {
            return LocalityContract(emptyList())
        }

        return LocalityContract(
            requiredLocalities.zip(other.requiredLocalities)
                .map { (thisRequirement, otherRequirement) ->
                    thisRequirement.union(otherRequirement)
                }
        )
    }
}
