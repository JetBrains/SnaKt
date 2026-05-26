/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.FunctionCall
import org.jetbrains.kotlin.formver.core.embeddings.expression.MethodCall

// TODO: Consider making a PureNonInlineNamedFunctionEmbedding and remove the pure distinction here
class NonInlineNamedFunctionSignature(val signature: NamedFunctionSignature) :
    CallableNamedSignature,
    NamedFunctionSignature by signature {

    fun insertCall(
        args: List<ExpEmbedding>
    ): ExpEmbedding = if (signature.isPure) {
        FunctionCall(signature, args)
    } else {
        MethodCall(signature, args)
    }

    override fun insertCall(
        args: List<ExpEmbedding>,
        ctx: StmtConversionContext,
    ): ExpEmbedding = insertCall(args)
}
