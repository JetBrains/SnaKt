/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.contract.plugin

import org.jetbrains.kotlin.formver.type.plugin.TypeFactJudgment

/**
 * Judgment for [FunctionTypeFact]s.
 *
 * @param TypeFact the type-fact of the contract.
 * @param typeJudgment the type-fact judgment to use for checking the compatibility of the parameters and result of the
 *  contracts.
 */
class FunctionTypeFactJudgment<TypeFact>(
    private val typeJudgment: TypeFactJudgment<TypeFact>,
) : TypeFactJudgment<FunctionTypeFact<TypeFact>?> {
    override fun satisfies(
        requiredTypeFact: FunctionTypeFact<TypeFact>?,
        actualTypeFact: FunctionTypeFact<TypeFact>?
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
