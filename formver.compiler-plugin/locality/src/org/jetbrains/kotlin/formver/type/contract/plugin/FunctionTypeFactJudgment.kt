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
 * @param typeFactJudgment the type-fact judgment to use for checking the compatibility of the type-facts and function
 * type-facts of the parameters and result.
 */
class FunctionTypeFactJudgment<TypeFact>(
    private val typeFactJudgment: TypeFactJudgment<TypeFact>,
) : TypeFactJudgment<FunctionTypeFact<TypeFact>?> {
    override fun satisfies(
        requiredTypeFact: FunctionTypeFact<TypeFact>?,
        actualTypeFact: FunctionTypeFact<TypeFact>?
    ): Boolean =
        when {
            requiredTypeFact == null -> true
            actualTypeFact == null -> false
            requiredTypeFact.parameterTypeFacts.size != actualTypeFact.parameterTypeFacts.size -> false
            else -> requiredTypeFact.parameterTypeFacts
                .zip(actualTypeFact.parameterTypeFacts)
                .all { (requiredElement, actualElement) ->
                    typeFactJudgment.satisfies(
                        requiredElement.typeFact,
                        actualElement.typeFact
                    ) && satisfies(
                        requiredElement.functionTypeFact,
                        actualElement.functionTypeFact
                    )
                } && satisfies(requiredTypeFact.resultFunctionTypeFact, actualTypeFact.resultFunctionTypeFact)
        }
}
