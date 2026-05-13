/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.FunctionCall
import org.jetbrains.kotlin.formver.core.embeddings.expression.PlaceholderVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.AdtTypeEmbeddingImpl
import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.asTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.embeddings.types.nullableAny
import org.jetbrains.kotlin.formver.core.names.ReturnVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName

class AdtEqualityEmbedding(val adtType: AdtTypeEmbeddingImpl) :
    CallableEmbedding, NamedFunctionSignature, GenericFunctionSignatureMixin() {

    override val name: SymbolicName = adtType.equalityFunctionName

    override val callableType: FunctionTypeEmbedding = FunctionTypeEmbedding(
        dispatchReceiverType = null,
        extensionReceiverType = null,
        paramTypes = listOf(adtType.asTypeEmbedding(), adtType.asTypeEmbedding()),
        returnType = buildType { boolean() },
        returnsUnique = false,
    )

    override val symbol: FirFunctionSymbol<*>
        get() = error("AdtEqualityEmbedding has no FIR symbol")

    override val isPure: Boolean = true

    override val returns: VariableEmbedding =
        PlaceholderVariableEmbedding(ReturnVariableName(0), buildType { nullableAny() })

    override fun insertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext): ExpEmbedding =
        FunctionCall(this, args)
}