/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.expression.AdtConstructorRef
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.AdtFieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.AdtTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.asTypeEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Method

class AdtConstructorEmbedding(
    val adtType: AdtTypeEmbedding,
    val fields: List<AdtFieldEmbedding>,
) : FunctionEmbedding {
    override val callableType: FunctionTypeEmbedding = FunctionTypeEmbedding(
        dispatchReceiverType = null,
        extensionReceiverType = null,
        paramTypes = fields.map { it.type },
        returnType = adtType.asTypeEmbedding(),
        returnsUnique = false,
    )

    override fun insertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext): ExpEmbedding =
        AdtConstructorRef(callableType.returnType, args)

    override fun viperMethod(ctx: TypeResolver): Method? = null
}
