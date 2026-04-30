/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.properties

import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.embeddings.callables.PureFunctionEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.*

/**
 * This Getter should be used for final fields that are immutable.
 * Such fields are replaced with viper function calls.
 */
class FinalFieldGetter(val getter: PureFunctionEmbedding) : GetterEmbedding {
    override fun getValue(receiver: ExpEmbedding, ctx: StmtConversionContext): ExpEmbedding = getter.insertCall(listOf(receiver), ctx)
}
