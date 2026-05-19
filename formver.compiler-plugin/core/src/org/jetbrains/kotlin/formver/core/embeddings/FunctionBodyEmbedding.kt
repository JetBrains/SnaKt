/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.callables.FullNamedFunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.callables.toViperMethod
import org.jetbrains.kotlin.formver.viper.ast.Method
import org.jetbrains.kotlin.formver.viper.ast.Stmt

sealed interface FunctionBodyConversionResult

data class FunctionBodyEmbedding(
    val viperBody: Stmt.Seqn,
) : FunctionBodyConversionResult {
    fun toViperMethod(signature: FullNamedFunctionSignature, ctx: TypeResolver): Method =
        signature.toViperMethod(viperBody, ctx)
}

data class InvalidFunctionBodyEmbedding(
    val source: KtSourceElement? = null,
) : FunctionBodyConversionResult
