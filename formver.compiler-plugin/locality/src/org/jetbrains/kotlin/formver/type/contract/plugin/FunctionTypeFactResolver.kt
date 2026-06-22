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

fun interface ConeTypeTypeFactResolver<TypeFact> {
    fun resolveTypeOf(type: ConeKotlinType, session: FirSession): TypeFact
}

/**
 * Resolves the type contract fact of an expression.
 *
 * @param TypeFact the type-fact class of the expression.
 * @param typeTypeFactResolver the resolver for resolving the type-fact from a parameter type.
 *
 * Returns `null` if the expression doesn't evaluate to a function, otherwise returns a type contract with the parameter
 * type-facts extracted by [typeTypeFactResolver].
 */
class TypeContractFactResolver<TypeFact>(
    private val typeTypeFactResolver: ConeTypeTypeFactResolver<TypeFact>,
) {
    fun resolveContractOf(type: ConeKotlinType, session: FirSession): FunctionTypeFact<TypeFact>? {
        val functionType = type.fullyExpandedType(session).lowerBoundIfFlexible() as? ConeClassLikeType
            ?: return null

        if (!functionType.isSomeFunctionType(session)) return null

        return FunctionTypeFact(
            parameters = functionType.valueParameterTypesIncludingReceiver(session).map { parameterType ->
                FunctionTypeFact.ParameterType(
                    typeFact = typeTypeFactResolver.resolveTypeOf(parameterType, session),
                    contract = resolveContractOf(parameterType, session),
                )
            },
            result = resolveContractOf(functionType.returnType(session), session),
        )
    }
}
