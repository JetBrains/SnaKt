/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.contract.plugin

import org.jetbrains.kotlin.formver.type.plugin.TypeFactJudgment

/**
 * Structural [TypeFactJudgment] over a nullable [FunctionTypeFact].
 *
 * A required function type-fact is satisfied by an actual one when:
 *
 * If the required contract is `null` (meaning no requirement at this position) this judgment returns true.
 *
 * If the required and actual contracts are non-null, and have the same arity, we pairwise check the type-facts of the
 * parameters:
 * - The actual parameter [TypeFact] satisfies the required one according to [typeFactJudgment].
 * - The nested required [FunctionTypeFact] recursively satisfies its actual counterpart through this same judgment.
 * Finally, this judgment checks that the actual result [TypeFact] satisfies the required one according to
 * [typeFactJudgment].
 *
 * Note that this function type-fact judgment is contravariant with respect to the parameter positions and covariant
 * with respect to the result. For example:
 *
 * required = ((global) -> ()) -> () // Function taking a function taking a global
 * actual = ((local) -> ()) -> () // Function taking a function taking a local
 * satisfies(required, actual) = false
 *
 * required = () -> ((global) -> ()) // Function returning a function taking a global
 * actual = () -> ((local) -> ()) // Function returning a function taking a local
 * satisfies(required, actual) = true
 *
 * @param TypeFact the type-fact class of the function type.
 * @param typeFactJudgment the type-fact judgment to use for checking the compatibility of the type-facts of the
 * parameters.
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
                        actualElement.functionTypeFact,
                        requiredElement.functionTypeFact
                    )
                } && satisfies(requiredTypeFact.resultFunctionTypeFact, actualTypeFact.resultFunctionTypeFact)
        }
}
