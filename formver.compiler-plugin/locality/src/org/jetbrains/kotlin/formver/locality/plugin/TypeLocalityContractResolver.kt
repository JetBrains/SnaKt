/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.valueParameterTypesIncludingReceiver

fun ConeKotlinType.resolveLocalityContract(session: FirSession): LocalityContract {
    val functionType = fullyExpandedType(session).lowerBoundIfFlexible() as? ConeClassLikeType
        ?: return null

    if (!functionType.isSomeFunctionType(session)) return null

    return functionType.valueParameterTypesIncludingReceiver(session).map(ConeKotlinType::locality)
}
