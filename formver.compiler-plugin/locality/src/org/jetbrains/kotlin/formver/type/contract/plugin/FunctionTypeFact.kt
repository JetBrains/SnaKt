/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.contract.plugin

/**
 * Recursive function type-fact representation based on [TypeFact].
 *
 * @param TypeFact the type of the function [ElementTypeFact]s.
 * @param parameterTypeFacts the list of function [ElementTypeFact]s referring to the input parameters.
 * @param resultFunctionTypeFact the function type-fact of the return value (if present).
 */
data class FunctionTypeFact<TypeFact>(
    val parameterTypeFacts: List<ElementTypeFact<TypeFact>>,
    val resultFunctionTypeFact: FunctionTypeFact<TypeFact>?,
) {
    data class ElementTypeFact<TypeFact>(
        val typeFact: TypeFact,
        val functionTypeFact: FunctionTypeFact<TypeFact>?
    )
}
