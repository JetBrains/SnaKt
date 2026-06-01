/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.contract.plugin

import org.jetbrains.kotlin.formver.type.plugin.TypeJudgment

class TypeContractJudgment<Type>(
    private val typeJudgment: TypeJudgment<Type>,
) : TypeJudgment<TypeContract<Type>> {
    override fun satisfies(requiredType: TypeContract<Type>, actualType: TypeContract<Type>): Boolean =
        when {
            requiredType == null -> true
            actualType == null -> false
            requiredType.parameters.size != actualType.parameters.size -> false
            else -> requiredType.parameters
                .zip(actualType.parameters).all { (requiredElement, actualElement) ->
                    typeJudgment.satisfies(actualElement.type, requiredElement.type) &&
                            satisfies(requiredElement.contract, actualElement.contract)
                } && satisfies(requiredType.result, actualType.result)
        }
}
