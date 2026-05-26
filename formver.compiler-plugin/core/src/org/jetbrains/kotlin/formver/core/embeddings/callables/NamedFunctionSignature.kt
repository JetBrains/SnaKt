/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.*

interface NamedFunctionSignature : FunctionSignature {
    val name: SymbolicName

    val symbol: FirFunctionSymbol<*>

    override val labelName: String
        get() = symbol.name.asString()
}

data class NamedFunctionSignatureImpl(
    override val name: SymbolicName,
    override val symbol: FirFunctionSymbol<*>,
    val functionSignature: FunctionSignature
) : NamedFunctionSignature, FunctionSignature by functionSignature {
    override val labelName: String = symbol.name.asString()
}

fun NamedFunctionSignature.toMethodCall(
    parameters: List<Exp>,
    target: Exp.LocalVar,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
) = Stmt.MethodCall(name, parameters, listOf(target), pos, info)

fun NamedFunctionSignature.toFuncApp(
    parameters: List<Exp>,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
) = Exp.FuncApp(name, parameters, Type.Ref, pos, info)


interface CallableNamedSignature : NamedFunctionSignature, CallableEmbedding
