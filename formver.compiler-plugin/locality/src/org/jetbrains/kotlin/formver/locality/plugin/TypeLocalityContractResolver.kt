/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.type

fun ConeKotlinType.resolveLocalityContract(session: FirSession): LocalityContract =
    if (isSomeFunctionType(session)) {
        typeArguments.map { typeArgument ->
            typeArgument.type?.locality
        }
    } else {
        null
    }
