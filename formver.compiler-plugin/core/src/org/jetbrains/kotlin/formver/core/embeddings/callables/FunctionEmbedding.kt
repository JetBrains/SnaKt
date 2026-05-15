/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.FunctionBodyConversionResult
import org.jetbrains.kotlin.formver.core.embeddings.FunctionBodyEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.InvalidFunctionBodyEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Function
import org.jetbrains.kotlin.formver.viper.ast.Method

interface FunctionEmbedding : CallableEmbedding {
    fun viperMethod(ctx: TypeResolver): Method?
}

interface PureFunctionEmbedding : CallableEmbedding {
    fun viperFunction(ctx: TypeResolver): Function?
}

/**
 * An embedding of a user-defined function.
 */
class UserFunctionEmbedding(private val callable: RichCallableEmbedding) : FunctionEmbedding,
    CallableEmbedding by callable {
    /**
     * The presence of the body indicates that the function should be verified, as opposed to simply having a declaration available
     */
    var body: FunctionBodyConversionResult? = null

    override fun viperMethod(ctx: TypeResolver): Method? = when (val currentBody = body) {
            is InvalidFunctionBodyEmbedding -> throw SnaktInternalException(
                currentBody.source,
                "Invalid function body detected in user-defined function"
            )

        is FunctionBodyEmbedding -> currentBody.toViperMethod(callable, ctx)
        else -> callable.toViperMethodHeader(ctx)
        }
}


/**
 * An embedding of a user-defined pure function
 */
class PureUserFunctionEmbedding(private val callable: RichCallableEmbedding) : PureFunctionEmbedding,
    CallableEmbedding by callable {

    var body: Exp? = null

    override fun viperFunction(ctx: TypeResolver): Function = callable.toViperFunction(ctx, body)
}
