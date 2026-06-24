/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.contract.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.formver.locality.plugin.locality
import org.jetbrains.kotlin.formver.type.contract.plugin.FunctionTypeFactResolver

private val LocalityTypeContractResolver = FunctionTypeFactResolver { type, _ -> type.locality }

fun ConeKotlinType.resolveLocalityContract(session: FirSession): LocalityContract? =
    LocalityTypeContractResolver.resolveContractOf(this, session)
