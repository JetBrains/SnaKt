/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.contract.plugin

/**
 * Recursive contract representation based on [Type].
 *
 * @param Type the type of the contract [ParameterType]s.
 * @param parameters the list of contract [ParameterType]s referring to the input parameters.
 * @param result the contract [ParameterType] referring to the return value.
 */
data class FunctionType<Type>(
    val parameters: List<ParameterType<Type>>,
    val result: FunctionType<Type>?,
) {
    data class ParameterType<Type>(
        val type: Type,
        val contract: FunctionType<Type>?
    )
}

typealias TypeContract<Type> = FunctionType<Type>?
