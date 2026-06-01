/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.properties

import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.callables.NonInlineFunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.callables.insertCall
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding

class CustomGetter(val getterMethod: NonInlineFunctionSignature) : GetterEmbedding {
    override fun getValue(
        receiver: ExpEmbedding,
        ctx: TypeResolver
    ): ExpEmbedding = getterMethod.insertCall(listOf(receiver))


    override fun getValueSimple(
        receiver : ExpEmbedding,
        ctx: TypeResolver,
    ): ExpEmbedding = getterMethod.insertCall(listOf(receiver))
}

class CustomSetter(val setterMethod: NonInlineFunctionSignature) : SetterEmbedding {
    override fun setValue(
        receiver: ExpEmbedding,
        value: ExpEmbedding,
        ctx: StmtConversionContext,
    ): ExpEmbedding = setterMethod.insertCall(listOf(receiver, value), ctx, setterMethod.callableType.returnType)
}
