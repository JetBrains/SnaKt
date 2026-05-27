/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.FunctionCall
import org.jetbrains.kotlin.formver.core.embeddings.expression.MethodCall
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.*

interface NamedFunctionSignature : FunctionSignature {
    val name: SymbolicName

    val symbol: FirFunctionSymbol<*>?

    override val labelName: String?
        get() = symbol?.name?.asString()
}

data class NamedFunctionSignatureImpl(
    val signature: FunctionSignature,
    override val name: SymbolicName,
    override val symbol: FirFunctionSymbol<*>?
) : NamedFunctionSignature, FunctionSignature by signature {
    override val labelName: String?
        get() = super<NamedFunctionSignature>.labelName
}

interface NamedCallableEmbedding : NamedFunctionSignature, CallableEmbedding

interface NonInlineCallable : NamedCallableEmbedding {
    override fun insertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext): ExpEmbedding = insertCall(args)

    // When the function does not need to be inlined, we do not need a context to call it. This is helpful, because
    // in some situations, we do not want to pass around the context (or we don't have access to it)
    fun insertCall(args: List<ExpEmbedding>): ExpEmbedding = if (isPure) {
        FunctionCall(this, args)
    } else {
        MethodCall(this, args)
    }
}

fun NonInlineCallable.toMethodCall(
    parameters: List<Exp>,
    target: Exp.LocalVar,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
) = Stmt.MethodCall(name, parameters, listOf(target), pos, info)

fun NonInlineCallable.toFuncApp(
    parameters: List<Exp>,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
) = Exp.FuncApp(name, parameters, Type.Ref, pos, info)


data class NonInlineCallableImpl(
    val functionSignature: NamedFunctionSignature,
    override val symbol: FirFunctionSymbol<*>?
) : NonInlineCallable, NamedFunctionSignature by functionSignature {
    override val labelName: String? = symbol?.name?.asString()
}
