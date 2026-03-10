/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.core.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding

/**
 * The symbol resolution data for a single method.
 *
 * Method converters are chained syntactically; the converter of a lambda has the method that the lambda is defined in as a parent.
 * In general, however, a callee inline function does *not* in general have its caller as a parent: this is because an inlined
 * function does not have access to the variables of its caller, so it does not make sense to have symbol resolution pass through it.
 *
 * We're using the term `MethodConverter` here for consistency with the `XConverter` implementing `XConversionContext`.
 * Really, this class doesn't do any conversion itself, it just provides information for the `StmtConverter`
 * to get its work done.
 *
 * Concretely, a [MethodConverter] owns:
 * - A [PropertyResolver] for local variables and loop labels within this scope.
 * - A [ParameterResolver] for formal parameters and receivers of this function.
 * - An optional [parent] for symbol lookup in lexically enclosing scopes (e.g. the outer
 *   function of a lambda). Resolution always tries the current scope first, then walks up.
 *
 * All [ProgramConversionContext] operations (type embedding, fresh variable production, etc.)
 * are delegated to [programCtx].
 *
 * @param programCtx The program-level context, shared across all methods in the program.
 * @param signature The signature of the function this converter represents.
 * @param paramResolver Resolves formal parameters and receivers for this function.
 * @param scopeDepth The initial scope depth for local variable tracking.
 * @param parent The enclosing [MethodConversionContext], if any (e.g. the outer function of a lambda).
 */
class MethodConverter(
    private val programCtx: ProgramConversionContext,
    override val signature: FunctionSignature,
    private val paramResolver: ParameterResolver,
    scopeDepth: ScopeIndex,
    private val parent: MethodConversionContext? = null,
) : MethodConversionContext, ProgramConversionContext by programCtx {

    // Tracks local variables and loop labels within the current scope chain.
    // Replaced on every scope entry/exit to maintain a stack-like structure.
    private var propertyResolver = PropertyResolver(scopeDepth)

    /**
     * True when the current scope does not allow creating new local variables —
     * i.e. we are inside a `forall` quantifier block where only pure expressions are valid.
     */
    override val isValidForForAllBlock: Boolean
        get() = !propertyResolver.canCreateLocals

    /**
     * Executes [action] inside a new inner scope, then restores the previous scope.
     * Used to bracket blocks (`{ ... }`) so that variables declared inside do not leak out.
     *
     * @param scopeDepth The [ScopeIndex] for the new inner scope.
     */
    override fun <R> withScopeImpl(scopeDepth: ScopeIndex, action: () -> R): R {
        propertyResolver = propertyResolver.innerScope(scopeDepth)
        val result = action()
        propertyResolver = propertyResolver.parent!!
        return result
    }

    /**
     * Associates a loop label with a unique index in the current scope.
     * Used later by [resolveLoopIndex] to retrieve the index for `break@label` / `continue@label`.
     */
    override fun addLoopIdentifier(labelName: String, index: Int) {
        propertyResolver = propertyResolver.addLoopIdentifier(labelName, index)
    }

    /**
     * Looks up the integer index assigned to a named loop label.
     * Throws if the label is not found — indicates a bug in FIR structure or conversion order.
     */
    override fun resolveLoopIndex(name: String): Int =
        propertyResolver.tryResolveLoopName(name) ?: throw IllegalArgumentException("Loop $name not found in scope.")

    /**
     * Resolves a local variable or property symbol to its [VariableEmbedding].
     * Searches the current scope first, then walks up to [parent] if not found.
     * Throws if the symbol is not in any enclosing scope.
     */
    override fun resolveLocal(symbol: FirVariableSymbol<*>): VariableEmbedding =
        propertyResolver.tryResolveLocalProperty(symbol) ?: parent?.resolveLocal(symbol)
        ?: throw IllegalArgumentException("Property ${symbol.name} not found in scope.")

    /**
     * Registers a local property declaration (a `val` or `var` in the function body).
     *
     * Special properties (those with an is-prefixed name) are mapped to fresh anonymous
     * variables. Regular properties are registered by symbol for later lookup via [resolveLocal].
     */
    override fun registerLocalProperty(symbol: FirPropertySymbol) {
        if (symbol.name.isSpecial)
            propertyResolver.registerSpecialProperty(symbol, freshAnonVar(embedType(symbol.resolvedReturnType)))
        else
            propertyResolver.registerLocalProperty(symbol, embedType(symbol.resolvedReturnType))
    }

    /**
     * Registers a local variable declaration (e.g. a `val` from a destructuring or `for` loop)
     * by symbol so it can be resolved later via [resolveLocal].
     */
    override fun registerLocalVariable(symbol: FirVariableSymbol<*>) {
        propertyResolver.registerLocalVariable(symbol, embedType(symbol.resolvedReturnType))
    }

    /**
     * Resolves a formal value parameter to its [ExpEmbedding].
     * Tries [paramResolver] first, then [parent] (for lambdas capturing outer parameters).
     * Throws if not found in any enclosing scope.
     */
    override fun resolveParameter(symbol: FirValueParameterSymbol): ExpEmbedding =
        paramResolver.tryResolveParameter(symbol) ?: parent?.resolveParameter(symbol)
        ?: throw IllegalArgumentException("Parameter ${symbol.name} not found in scope.")

    /**
     * Returns the dispatch receiver (`this`) embedding for this scope,
     * or delegates to [parent] if this scope has no dispatch receiver.
     */
    override fun resolveDispatchReceiver(): ExpEmbedding? =
        paramResolver.tryResolveDispatchReceiver() ?: parent?.resolveDispatchReceiver()

    /**
     * Returns the extension receiver for the scope identified by [labelName],
     * or delegates to [parent] if this scope's label does not match.
     * Used for labelled extension-receiver access (e.g. `this@outer`).
     */
    override fun resolveExtensionReceiver(labelName: String): ExpEmbedding? =
        paramResolver.tryResolveExtensionReceiver(labelName) ?: parent?.resolveExtensionReceiver(labelName)

    /** The [ReturnTarget] for an unlabelled `return` in this scope. */
    override val defaultResolvedReturnTarget = paramResolver.defaultResolvedReturnTarget

    /**
     * Resolves a named `return@label` to a [ReturnTarget].
     * Tries this scope's [paramResolver] first, then walks up to [parent].
     * Returns null if no enclosing scope owns the label.
     */
    override fun resolveNamedReturnTarget(labelName: String): ReturnTarget? =
        paramResolver.resolveNamedReturnTarget(labelName) ?: parent?.resolveNamedReturnTarget(labelName)

    /**
     * Yields all [VariableEmbedding]s visible in this scope and all enclosing scopes:
     * local properties (from [PropertyResolver]), formal parameters (from [ParameterResolver]),
     * and then the parent's variables recursively.
     *
     * Used when building Viper `method` declarations that must list all local variable declarations.
     */
    override fun retrievePropertiesAndParameters(): Sequence<VariableEmbedding> = sequence {
        yieldAll(propertyResolver.retrieveAllProperties())
        yieldAll(paramResolver.retrieveAllParams())
        parent?.retrievePropertiesAndParameters()?.let { yieldAll(it) }
    }
}