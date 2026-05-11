/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.FunctionBodyEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Function
import org.jetbrains.kotlin.formver.viper.ast.Method

interface FunctionEmbedding : CallableEmbedding {
    fun viperMethod(ctx: TypeResolver, body: FunctionBodyEmbedding?): Method?
}

interface PureFunctionEmbedding : CallableEmbedding {
    fun viperFunction(ctx: TypeResolver, body: Exp?): Function?
}

/**
 * An embedding of a user-defined function.
 */
class UserFunctionEmbedding(private val callable: RichCallableEmbedding) : FunctionEmbedding,
    CallableEmbedding by callable {
    override fun viperMethod(ctx: TypeResolver, body: FunctionBodyEmbedding?): Method? =
        body?.toViperMethod(callable, ctx) ?: callable.toViperMethodHeader(ctx)
}


/**
 * An embedding of a user-defined pure function
 */
class PureUserFunctionEmbedding(private val callable: RichCallableEmbedding) : PureFunctionEmbedding,
    CallableEmbedding by callable {
    override fun viperFunction(ctx: TypeResolver, body: Exp?): Function = callable.toViperFunction(ctx, body)
}
