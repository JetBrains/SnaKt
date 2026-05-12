/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.formver.core.conversion.ProgramConversionContext

enum class MemberEmbeddingPolicy {
    FUNCTION,
    BACKING_FIELD,
    PRIVATE_BACKING_FIELD,
    METHOD,
}


fun scopePolicy(property: FirPropertySymbol, ctx: ProgramConversionContext): MemberEmbeddingPolicy {
    val wellBehaved = ctx.isWellBehavedProperty(property)
    val isPrivate = Visibilities.isPrivate(property.visibility)
    val isMutable = property.isVar

    return when {
        wellBehaved && isMutable && isPrivate -> MemberEmbeddingPolicy.PRIVATE_BACKING_FIELD
        wellBehaved && isMutable -> MemberEmbeddingPolicy.BACKING_FIELD
        wellBehaved && !isMutable -> MemberEmbeddingPolicy.FUNCTION
        else -> MemberEmbeddingPolicy.METHOD
    }
}
