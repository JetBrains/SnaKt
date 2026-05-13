/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.diagnostics.rendering.Renderer

typealias LocalityContract = List<Locality>?

fun LocalityContract.join(other: LocalityContract): LocalityContract =
    when {
        this == null -> other
        other == null -> this
        size != other.size -> null
        else -> this.zip(other).map { (thisLocality, otherLocality) ->
            thisLocality.meet(otherLocality)
        }
    }

fun LocalityContract.accept(other: LocalityContract): Boolean =
    when {
        this == null || other == null -> true
        size != other.size -> true
        else -> zip(other).all { (expectedLocality, actualLocality) ->
            actualLocality.accepts(expectedLocality)
        }
    }

val LocalityContractRenderer = Renderer<LocalityContract> { contract ->
    contract?.joinToString(prefix = "[", postfix = "]") { locality ->
        when (locality) {
            null -> "global"
            LocalityAttribute -> "local"
        }
    } ?: "unknown"
}
