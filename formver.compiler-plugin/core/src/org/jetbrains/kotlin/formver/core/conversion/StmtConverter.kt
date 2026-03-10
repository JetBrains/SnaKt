/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.formver.core.embeddings.LabelEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.withPosition
import org.jetbrains.kotlin.formver.core.names.BreakLabelName
import org.jetbrains.kotlin.formver.core.names.ContinueLabelName
import org.jetbrains.kotlin.formver.viper.SymbolicName

/**
 * Concrete implementation of [StmtConversionContext] for converting a single function body.
 *
 * Tracks the results of converting a block of statements.
 * Kotlin statements, declarations, and expressions do not map to Viper ones one-to-one.
 * Converting a statement with multiple function calls may require storing the
 * intermediate results, which requires introducing new names. We thus need a
 * shared context for finding fresh variable names.
 *
 * `StmtConverter` is immutable: context changes (entering a new scope, a `when` subject,
 * a new while-loop index, etc.) are modelled by `copy`ing the data class with updated fields.
 * This means every `withX { }` helper produces a fresh `StmtConverter` for the duration of
 * the block and then discards it, leaving the outer converter unchanged.
 *
 * All [MethodConversionContext] operations (symbol resolution, type embedding, etc.) are
 * delegated to [methodCtx].
 *
 * NOTE: If you add parameters, be sure to update the `withResultFactory` function!
 *
 * @param methodCtx The [MethodConversionContext] providing symbol resolution for this function.
 * @param whileIndex The index of the innermost active while-loop, used to generate unique
 *   `break`/`continue` label names.
 * @param whenSubject The temporary variable holding the `when` expression subject, or null.
 * @param checkedSafeCallSubject The pre-evaluated receiver of the current `?.` safe call, or null.
 * @param scopeIndex The scope depth tracker for local variable scoping.
 * @param activeCatchLabels The stack of active exception-handler entry labels for try-catch nesting.
 */
data class StmtConverter(
    private val methodCtx: MethodConversionContext,
    private val whileIndex: Int = 0,
    override val whenSubject: VariableEmbedding? = null,
    override val checkedSafeCallSubject: ExpEmbedding? = null,
    private val scopeIndex: ScopeIndex = ScopeIndex.Indexed(0),
    override val activeCatchLabels: List<LabelEmbedding> = listOf(),
) : StmtConversionContext, MethodConversionContext by methodCtx {

    /**
     * Dispatches [stmt] to [StmtConversionVisitor] and attaches the source position to the result.
     * This is the single entry point for all FIR-to-embedding conversion within a function body.
     */
    override fun convert(stmt: FirStatement): ExpEmbedding =
        stmt.accept(StmtConversionVisitor, this).withPosition(stmt.source)

    /** Opens a new scope that allows local variable declarations. */
    override fun <R> withNewScope(action: StmtConversionContext.() -> R): R = withNewScopeImpl { action() }

    /**
     * Opens a scope that forbids local variable creation (e.g. inside a `forall` quantifier block).
     * Used to enforce that quantifier bodies contain only pure expressions.
     */
    override fun <R> withNoScope(action: StmtConversionContext.() -> R): R =
        withNewScopeImpl(needsScope = false, action)

    /**
     * Switches to a new [MethodConversionContext] created by [factory] for the duration of [action].
     * Used when entering an inlined function body or a lambda, where a different parameter resolver
     * and scope depth is required. The outer converter is restored after [action] completes.
     */
    override fun <R> withMethodCtx(factory: MethodContextFactory, action: StmtConversionContext.() -> R): R {
        return copy(methodCtx = factory.create(this, scopeIndex)).run {
            if (scopeIndex is ScopeIndex.Indexed) withNewScope(action)
            else withScopeImpl(ScopeIndex.NoScope) { action() }
        }
    }

    /**
     * Resolves the while-loop index for a given optional label name.
     * If [targetName] is provided (labelled `break`/`continue`), looks up the stored index;
     * otherwise returns the current [whileIndex].
     */
    private fun resolveWhileIndex(targetName: String?) =
        if (targetName != null) {
            resolveLoopIndex(targetName)
        } else {
            whileIndex
        }

    /** Returns the Viper label name used as the `continue` target for the loop identified by [targetName]. */
    override fun continueLabelName(targetName: String?): SymbolicName {
        val index = resolveWhileIndex(targetName)
        return ContinueLabelName(index)
    }

    /** Returns the Viper label name used as the `break` target for the loop identified by [targetName]. */
    override fun breakLabelName(targetName: String?): SymbolicName {
        val index = resolveWhileIndex(targetName)
        return BreakLabelName(index)
    }

    /**
     * Registers a Kotlin loop label in the enclosing [MethodConversionContext] so that
     * labelled `break`/`continue` expressions can look up the correct while-loop index later.
     */
    override fun addLoopName(targetName: String) {
        methodCtx.addLoopIdentifier(targetName, whileIndex)
    }

    /**
     * Executes [action] in a new scope with a fresh while-loop index.
     * If [label] is present, the label name is registered so labelled `break`/`continue` work.
     * The new scope ensures loop-local variables do not leak out.
     */
    override fun <R> withFreshWhile(label: FirLabel?, action: StmtConversionContext.() -> R): R =
        withNewScopeImpl {
            val freshIndex = whileIndexProducer.getFresh()
            val ctx = copy(whileIndex = freshIndex)
            label?.let { ctx.addLoopName(it.name) }
            ctx.action()
        }

    /**
     * Executes [action] with [subject] set as the active `when`-expression subject variable.
     * Branches inside the `when` read [whenSubject] instead of re-evaluating the original expression.
     */
    override fun <R> withWhenSubject(subject: VariableEmbedding?, action: StmtConversionContext.() -> R): R =
        copy(whenSubject = subject).action()

    /**
     * Executes [action] with [subject] stored as the safe-call receiver.
     * Prevents re-evaluation of the `?.` receiver when both the null-check and the method call
     * need to reference the same value.
     */
    override fun <R> withCheckedSafeCallSubject(subject: ExpEmbedding?, action: StmtConversionContext.() -> R): R =
        copy(checkedSafeCallSubject = subject).action()

    /**
     * Registers [catches] as the active exception handlers and executes [action].
     *
     * For each catch clause a fresh entry [LabelEmbedding] is produced; a shared exit label is
     * also created. The new labels are appended to [activeCatchLabels] so that `throw` expressions
     * encountered inside the try body can emit non-deterministic jumps to each handler.
     *
     * Returns the [CatchBlockListData] (which carries the catch/exit labels) alongside the
     * action result so the caller can assemble the full try-catch `Block` embedding.
     */
    override fun <R> withCatches(
        catches: List<FirCatch>,
        action: StmtConversionContext.(catchBlockListData: CatchBlockListData) -> R,
    ): Pair<CatchBlockListData, R> {
        val newCatchLabels = catches.map { LabelEmbedding(catchLabelNameProducer.getFresh()) }
        val exitLabel = LabelEmbedding(tryExitLabelNameProducer.getFresh())
        val ctx = copy(activeCatchLabels = activeCatchLabels + newCatchLabels)
        val catchBlockListData =
            CatchBlockListData(
                exitLabel,
                newCatchLabels.zip(catches).map { (label, firCatch) -> CatchBlockData(label, firCatch) })
        val result = ctx.action(catchBlockListData)
        return Pair(catchBlockListData, result)
    }

    /**
     * Internal helper that creates a child [StmtConverter] with a new [ScopeIndex] and executes
     * [action] inside it. If [needsScope] is false, [ScopeIndex.NoScope] is used (forbids locals).
     *
     * The `withScopeImpl` call on the [MethodConversionContext] side ensures the [PropertyResolver]
     * also enters and exits the corresponding scope, so variable registrations are properly scoped.
     */
    private fun <R> withNewScopeImpl(needsScope: Boolean = true, action: StmtConverter.() -> R): R {
        val newScopeIndex = if (needsScope) scopeIndexProducer.getFresh() else ScopeIndex.NoScope
        val inner = copy(scopeIndex = newScopeIndex)
        var result: R? = null
        inner.withScopeImpl(newScopeIndex) { result = inner.action() }
        return result!!
    }
}
