/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.formver.common.ErrorCollector
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.core.embeddings.callables.CallableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.callables.PureUserFunctionEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.properties.PropertyEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.names.CatchLabelName
import org.jetbrains.kotlin.formver.core.names.TryExitLabelName
import org.jetbrains.kotlin.formver.viper.NameResolver

interface ProgramConversionContext {
    val session: FirSession
    val config: PluginConfiguration
    val errorCollector: ErrorCollector

    val whileIndexProducer: SimpleFreshEntityProducer<Int>
    val catchLabelNameProducer: SimpleFreshEntityProducer<CatchLabelName>
    val tryExitLabelNameProducer: SimpleFreshEntityProducer<TryExitLabelName>
    val scopeIndexProducer: SimpleFreshEntityProducer<ScopeIndex.Indexed>

    val anonVarProducer: FreshEntityProducer<AnonymousVariableEmbedding, TypeEmbedding>
    val anonBuiltinVarProducer: FreshEntityProducer<AnonymousBuiltinVariableEmbedding, TypeEmbedding>
    val returnTargetProducer: FreshEntityProducer<ReturnTarget, TypeEmbedding>
    val nameResolver: NameResolver
    val typeResolver: TypeResolver
    val convertedBodyResolver: ConvertedBodyResolver
    val linearizedBodyResolver: LinearizedBodyResolver

    fun embedFunction(symbol: FirFunctionSymbol<*>): CallableEmbedding
    fun embedPureFunction(symbol: FirFunctionSymbol<*>): PureUserFunctionEmbedding
    fun embedAnyFunction(symbol: FirFunctionSymbol<*>): CallableEmbedding
    fun embedFunctionSignature(symbol: FirFunctionSymbol<*>): Pair<ReturnTarget,FunctionSignature>
    fun embedType(type: ConeKotlinType): TypeEmbedding
    fun embedFunctionPretype(symbol: FirFunctionSymbol<*>): FunctionTypeEmbedding
    fun embedType(exp: FirExpression): TypeEmbedding = embedType(exp.resolvedType)
    fun embedProperty(symbol: FirPropertySymbol): PropertyEmbedding

    /**
     * Lazily creates and returns a structural equals embedding for an ADT type,
     * or `null` if [receiverType] is not an ADT.
     */
    fun tryEmbedAdtEquals(receiverType: ConeKotlinType): CallableEmbedding? = null

    /**
     * Returns true if the property has default behavior. That is:
     * It cannot be overwritten and does not have custom getters or setters
     */
    fun isGuaranteedDefaultProperty(symbol: FirPropertySymbol): Boolean
}

fun ProgramConversionContext.freshAnonVar(type: TypeEmbedding): VariableEmbedding = anonVarProducer.getFresh(type)
fun ProgramConversionContext.freshAnonBuiltinVar(type: TypeEmbedding): VariableEmbedding =
    anonBuiltinVarProducer.getFresh(type)

/** == desugarization: if [left] is nullable, produces `(left != null && left.equals(right)) || (left == null && right == null)` */
fun desugarEqualsCall(
    left: ExpEmbedding,
    right: ExpEmbedding,
    equalsCallable: CallableEmbedding,
    ctx: StmtConversionContext,
): ExpEmbedding {
    if (!left.type.flags.nullable) {
        return equalsCallable.insertCall(listOf(left, right), ctx)
    }
    val nonNullableLeftType = left.type.getNonNullable()
    return share(left) { sharedLeft ->
        share(right) { sharedRight ->
            SequentialOr(
                SequentialAnd(
                    sharedLeft.notNullCmp(),
                    equalsCallable.insertCall(listOf(sharedLeft.withType(nonNullableLeftType), sharedRight), ctx),
                ),
                SequentialAnd(
                    EqCmp(sharedLeft, NullLit),
                    EqCmp(sharedRight, NullLit),
                ),
            )
        }
    }
}

/**
 * Pure-safe variant of [desugarEqualsCall] for use in pure Viper function bodies.
 * Uses [If] (ternary) instead of [share]/[SequentialAnd]/[SequentialOr], which are
 * impure constructs that don't linearize correctly in pure contexts.
 */
fun pureDesugarEqualsCall(
    left: ExpEmbedding,
    right: ExpEmbedding,
    equalsCallable: CallableEmbedding,
    ctx: StmtConversionContext,
): ExpEmbedding {
    if (!left.type.flags.nullable) {
        return equalsCallable.insertCall(listOf(left, right), ctx)
    }
    val nonNullableLeftType = left.type.getNonNullable()
    return If(
        left.notNullCmp(),
        equalsCallable.insertCall(listOf(left.withType(nonNullableLeftType), right), ctx),
        EqCmp(right, NullLit),
        buildType { boolean() },
    )
}
