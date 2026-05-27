/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding

interface NamedFunctionSignatureWithContract : NamedFunctionSignature {
    /**
     * Preconditions of function in form of `ExpEmbedding`s with type `boolType()`.
     */
    val preconditions: List<ExpEmbedding>

    /**
     * Postconditions of function in form of `ExpEmbedding`s with type `boolType()`.
     */
    val postconditions: List<ExpEmbedding>

    val declarationSource: KtSourceElement?
}
