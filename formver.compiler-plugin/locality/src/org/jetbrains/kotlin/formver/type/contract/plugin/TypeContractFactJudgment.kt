/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.contract.plugin

import org.jetbrains.kotlin.formver.type.plugin.TypeFactJudgment

class TypeContractFactJudgment<Type>(
    private val typeJudgment: TypeFactJudgment<Type>,
) : TypeFactJudgment<TypeContractFact<Type>?> {
    override fun satisfies(
        requiredTypeFact: TypeContractFact<Type>?,
        actualTypeFact: TypeContractFact<Type>?
    ): Boolean =
        when {
            requiredTypeFact == null -> true
            actualTypeFact == null -> false
            requiredTypeFact.parameters.size != actualTypeFact.parameters.size -> false
            else -> requiredTypeFact.parameters
                .zip(actualTypeFact.parameters).all { (requiredElement, actualElement) ->
                    typeJudgment.satisfies(actualElement.type, requiredElement.type) &&
                            satisfies(requiredElement.contract, actualElement.contract)
                } && satisfies(requiredTypeFact.result, actualTypeFact.result)
        }
}
