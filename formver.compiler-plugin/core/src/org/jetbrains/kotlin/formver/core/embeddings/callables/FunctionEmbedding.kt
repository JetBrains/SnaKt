/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Function

/**
 * Marker for embeddings that live in the `methods` bucket of a `ProgramConverter`.
 * The hierarchy is sealed so renderers can dispatch over the leaves exhaustively.
 */
sealed interface FunctionEmbedding : CallableEmbedding

/**
 * An embedding of a user-defined function.
 */
class UserFunctionEmbedding(val callable: RichCallableEmbedding) : FunctionEmbedding,
    CallableEmbedding by callable


/**
 * An embedding of a user-defined pure function.
 */
class PureUserFunctionEmbedding(val callable: RichCallableEmbedding) : CallableEmbedding by callable {
    fun viperFunction(ctx: TypeResolver, body: Exp?): Function = callable.toViperFunction(ctx, body)
}

/**
 * The underlying user-function callable that this embedding renders to, if any.
 *
 * Returns `null` for `FullySpecialKotlinFunction`s (which never emit a Viper method) and for
 * `PartiallySpecialKotlinFunction`s whose `baseEmbedding` has not been initialised.
 */
fun FunctionEmbedding.userCallable(): RichCallableEmbedding? = when (this) {
    is UserFunctionEmbedding -> callable
    is FullySpecialKotlinFunction -> null
    is PartiallySpecialKotlinFunction -> (baseEmbedding as? UserFunctionEmbedding)?.callable
}
