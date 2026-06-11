/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.contract.plugin

/**
 * Recursive contract type-fact representation based on [TypeFact].
 *
 * @param TypeFact the type of the contract [ParameterType]s.
 * @param parameters the list of contract [ParameterType]s referring to the input parameters.
 * @param result the contract type-fact of the return value (if present).
 */
data class TypeContractFact<TypeFact>(
    val parameters: List<ParameterType<TypeFact>>,
    val result: TypeContractFact<TypeFact>?,
) {
    data class ParameterType<Type>(
        val type: Type,
        val contract: TypeContractFact<Type>?
    )
}
