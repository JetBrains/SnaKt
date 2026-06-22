/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.contract.plugin

import org.jetbrains.kotlin.formver.type.plugin.TypeFactIntersector
import org.jetbrains.kotlin.formver.type.plugin.TypeFactUnifier

/**
 * Implementation of a [TypeFactUnifier] for a nullable [FunctionTypeFact].
 *
 * @param TypeFact the type-fact class of the elements of the contract.
 * @param typeIntersector the type intersector to use for intersecting the input types of the elements.
 *
 * If both contracts are non-null, this unifier intersects the type-facts of the parameters between the unified
 * contracts, while also joining the type-fact contract of the results.
 *
 * If contracts have mismatched arities, the resulting contract will not specify parameter type-facts, but only the
 * result type-fact. For example:
 *
 * c1 = (t1, t2, t3) -> (t4)
 * c2 = (d1, d2) -> (d3)
 * c1.join(c2) = (t1.meet(d1), t2.meet(d2)) -> (t4.join(d3))
 *
 * This is consistent with the behavior of Kotlin's function-type unification.
 *
 * `val f: Function<Char> = if (false) { x: Int -> 'a' } else { x: Int, y: Int -> 'b'}`
 */
class FunctionTypeFactUnifier<TypeFact>(
    private val typeIntersector: TypeFactIntersector<TypeFact>,
) : TypeFactUnifier<FunctionTypeFact<TypeFact>?> {
    override fun join(left: FunctionTypeFact<TypeFact>?, right: FunctionTypeFact<TypeFact>?): FunctionTypeFact<TypeFact>? =
        when {
            left == null -> right
            right == null -> left
            else -> FunctionTypeFact(
                if (left.parameters.size != right.parameters.size) {
                    emptyList()
                } else {
                    left.parameters.zip(right.parameters).map { (leftElement, rightElement) ->
                        FunctionTypeFact.ParameterType(
                            typeIntersector.meet(leftElement.typeFact, rightElement.typeFact),
                            join(leftElement.contract, rightElement.contract)
                        )
                    }
                },
                join(left.result, right.result)
            )
        }
}
