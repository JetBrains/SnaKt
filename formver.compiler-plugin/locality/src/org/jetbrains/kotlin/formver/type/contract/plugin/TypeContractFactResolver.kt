/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.contract.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.returnType
import org.jetbrains.kotlin.fir.types.valueParameterTypesIncludingReceiver

fun interface ConeTypeTypeFactResolver<Type> {
    fun resolveTypeOf(type: ConeKotlinType, session: FirSession): Type
}

class TypeContractResolver<Type>(
    private val typeResolver: ConeTypeTypeFactResolver<Type>,
) {
    fun resolveContractOf(type: ConeKotlinType, session: FirSession): TypeContractFact<Type>? {
        val functionType = type.fullyExpandedType(session).lowerBoundIfFlexible() as? ConeClassLikeType
            ?: return null

        if (!functionType.isSomeFunctionType(session)) return null

        return TypeContractFact(
            parameters = functionType.valueParameterTypesIncludingReceiver(session).map { parameterType ->
                TypeContractFact.ParameterType(
                    type = typeResolver.resolveTypeOf(parameterType, session),
                    contract = resolveContractOf(parameterType, session),
                )
            },
            result = resolveContractOf(functionType.returnType(session), session),
        )
    }
}
