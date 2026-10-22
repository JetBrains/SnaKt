/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.isBoolean
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.embeddings.FunctionBodyEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.LabelEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.callables.NamedFunctionSignatureWithContract
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.isInvariantBuilderFunctionNamed
import org.jetbrains.kotlin.formver.core.linearization.*
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Exp

/**
 * Interface for statement conversion.
 *
 * Naming convention:
 * - Functions that return a new `StmtConversionContext` should describe what change they make (`addResult`, `removeResult`...)
 * - Functions that take a lambda to execute should describe what extra state the lambda will have (`withResult`...)
 */
interface StmtConversionContext : MethodConversionContext {
    val whenSubject: VariableEmbedding?

    /**
     * In a safe call `callSubject?.foo()` we evaluate the call subject first to check for nullness.
     * In case it is not null, we evaluate the call to `callSubject.foo()`. Here we don't want to evaluate
     * the `callSubject` again to we store it in the `StmtConversionContext`.
     */
    val checkedSafeCallSubject: ExpEmbedding?
    val activeCatchLabels: List<LabelEmbedding>

    fun continueLabelName(targetName: String? = null): SymbolicName
    fun breakLabelName(targetName: String? = null): SymbolicName
    fun addLoopName(targetName: String)
    fun convert(stmt: FirStatement): ExpEmbedding

    fun <R> withNewScope(action: StmtConversionContext.() -> R): R
    fun <R> withNoScope(action: StmtConversionContext.() -> R): R
    fun <R> withMethodCtx(factory: MethodContextFactory, action: StmtConversionContext.() -> R): R

    fun <R> withFreshWhile(label: FirLabel?, action: StmtConversionContext.() -> R): R
    fun <R> withWhenSubject(subject: VariableEmbedding?, action: StmtConversionContext.() -> R): R
    fun <R> withCheckedSafeCallSubject(subject: ExpEmbedding?, action: StmtConversionContext.() -> R): R
    fun <R> withCatches(
        catches: List<FirCatch>,
        action: StmtConversionContext.(catchBlockListData: CatchBlockListData) -> R,
    ): Pair<CatchBlockListData, R>
}

fun StmtConversionContext.convertImpureBody(
    declaration: FirSimpleFunction,
    signature: NamedFunctionSignatureWithContract,
    returnTarget: ReturnTarget,
): ConvertedMethodBody? {
    val firBody = declaration.body ?: return null
    val body = convert(firBody)
    val returnLabel = returnTarget.label ?: throw SnaktInternalException(
        declaration.source, "Return target label not found for method ${declaration.name}"
    )
    val bodyExp = FunctionExp(signature, body, returnLabel)
    return ConvertedMethodBody(bodyExp, returnTarget)
}

fun StmtConversionContext.convertPureBody(declaration: FirSimpleFunction): ExpEmbedding {
    val firBody = declaration.body ?: throw SnaktInternalException(
        declaration.source,
        "Pure functions expect a function body to exist"
    )
    return convert(firBody)
}

fun ProgramConversionContext.linearizeImpureBody(
    source: KtSourceElement?,
    converted: ConvertedMethodBody,
): FunctionBodyEmbedding {
    val seqnBuilder = SeqnBuilder(source)
    val linearizer =
        Linearizer(SharedLinearizationState(anonVarProducer), seqnBuilder, source, typeResolver)
    converted.bodyExp.toLinearizable(source).toViperUnusedResult(linearizer)
    // note: we must guarantee somewhere that returned value is Unit
    // as we may not encounter any `return` statement in the body
    converted.returnTarget.variable.withIsUnitInvariantIfUnit(typeResolver)
        .toLinearizable(source).toViperUnusedResult(linearizer)
    return FunctionBodyEmbedding(seqnBuilder.block)
}

fun ProgramConversionContext.linearizePureBody(
    source: KtSourceElement?,
    body: ExpEmbedding,
): Exp {
    val pureFunBodyLinearizer = PureFunBodyLinearizer(
        source,
        SharedLinearizationState(anonVarProducer),
        SsaConverter(source),
        typeResolver
    )
    body.toLinearizable(source).toViperUnusedResult(pureFunBodyLinearizer)
    return pureFunBodyLinearizer.constructExpression()
}

private const val INVALID_STATEMENT_MSG =
    "Every statement in invariant block must be a pure boolean invariant."

data class InvariantsAndTriggers(
    val invariants: List<ExpEmbedding>,
    val triggers: List<ExpEmbedding>
)

private fun FirBlock.isEmptyLambdaBody(): Boolean {
    if (statements.isEmpty()) return false
    return (statements.size == 1 && (statements.first() as? FirReturnExpression)?.result?.resolvedType?.isUnit ?: false)
}

fun StmtConversionContext.collectInvariants(block: FirBlock) = buildList {
    if (block.isEmptyLambdaBody()) {
        return@buildList
    }
    block.statements.forEach { stmt ->
        check(stmt is FirExpression && stmt.resolvedType.isBoolean) {
            INVALID_STATEMENT_MSG
        }
        add(stmt.accept(StmtConversionVisitor, this@collectInvariants))
    }
}

/**
 * Attempts to extract trigger expressions from a triggers() function call.
 * Returns the list of trigger expressions if this is a triggers() call, or null otherwise.
 */
private fun StmtConversionContext.tryExtractTriggers(stmt: FirStatement): List<ExpEmbedding>? {
    if (stmt !is FirFunctionCall) return null

    val symbol = stmt.toResolvedCallableSymbol() as? FirFunctionSymbol<*>
    if (symbol?.isInvariantBuilderFunctionNamed("triggers") != true) return null

    val varargs = stmt.arguments.firstOrNull() as? FirVarargArgumentsExpression
        ?: throw IllegalArgumentException("triggers() function must have a single varargs parameter.")

    // TODO: check whether trigger is valid in Viper.
    return varargs.arguments.map { expr ->
        expr.accept(StmtConversionVisitor, this)
    }
}

fun StmtConversionContext.collectInvariantsAndTriggers(block: FirBlock): InvariantsAndTriggers {
    val invariants = mutableListOf<ExpEmbedding>()
    val triggers = mutableListOf<ExpEmbedding>()

    block.statements.forEach { stmt ->
        val extractedTriggers = tryExtractTriggers(stmt)
        if (extractedTriggers != null) {
            triggers.addAll(extractedTriggers)
            return@forEach
        }

        // Otherwise, treat as invariant
        check(stmt is FirExpression && stmt.resolvedType.isBoolean) {
            INVALID_STATEMENT_MSG
        }
        invariants.add(stmt.accept(StmtConversionVisitor, this))
    }

    return InvariantsAndTriggers(invariants, triggers)
}
