/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.contract.plugin

import org.jetbrains.kotlin.formver.type.plugin.TypeIntersector
import org.jetbrains.kotlin.formver.type.plugin.TypeUnifier

/**
 * Implementation of a [org.jetbrains.kotlin.formver.type.TypeUnifier] for a [FunctionType].
 *
 * This implementation intersects the types of the parameters between the unified contracts.
 *
 * @param Type the type class of the elements of the contract.
 * @param typeIntersector the type intersector to use for intersecting the input types of the elements.
 */
class TypeContractUnifier<Type>(
    private val typeIntersector: TypeIntersector<Type>,
) : TypeUnifier<FunctionType<Type>?> {
    override fun join(left: FunctionType<Type>?, right: FunctionType<Type>?): FunctionType<Type>? =
        when {
            left == null -> right
            right == null -> left
            else -> FunctionType(
                left.parameters.zip(right.parameters).map { (leftElement, rightElement) ->
                    FunctionType.ParameterType(
                        typeIntersector.meet(leftElement.type, rightElement.type),
                        join(leftElement.contract, rightElement.contract)
                    )
                },
                join(left.result, right.result)
            )
        }
}
