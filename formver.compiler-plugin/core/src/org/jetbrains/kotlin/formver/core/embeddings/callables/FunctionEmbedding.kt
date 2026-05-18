/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Function

/**
 * An embedding of a user-defined function.
 */
class UserFunctionEmbedding(val callable: RichCallableEmbedding) : CallableEmbedding by callable


/**
 * An embedding of a user-defined pure function.
 */
class PureUserFunctionEmbedding(val callable: RichCallableEmbedding) : CallableEmbedding by callable {
    fun viperFunction(ctx: TypeResolver, body: Exp?): Function = callable.toViperFunction(ctx, body)
}
