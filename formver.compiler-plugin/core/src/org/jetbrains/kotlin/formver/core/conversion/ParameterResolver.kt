/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.core.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.underlyingVariable
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

/**
 * Name resolver for parameters and return values and labels.
 *
 * Since parameter names may map to lambda embeddings, we use `embed` for those for consistency.
 *
 * During body conversion the statement converter needs to map FIR symbols (value parameters,
 * receivers) to their corresponding [ExpEmbedding]s. This interface abstracts over the three
 * contexts in which that mapping differs:
 *
 * - **Regular functions** ([RootParameterResolver]): parameters come directly from the
 *   [FunctionSignature] and are matched by FIR symbol.
 * - **Specification embedding** ([SubstitutedReturnParameterResolver]): same as above, but the
 *   special `result` parameter is replaced by a concrete return [VariableEmbedding] so that
 *   postconditions can refer to the actual return value.
 * - **Inlined call sites** ([InlineParameterResolver]): parameters are replaced by the
 *   caller-provided argument expressions, keyed via [SubstitutedArgument].
 */
interface ParameterResolver {
    /** Looks up the embedding for a value parameter by its FIR symbol, or null if not found. */
    fun tryResolveParameter(symbol: FirValueParameterSymbol): ExpEmbedding?

    /** Returns the dispatch receiver (`this`) embedding, or null if the function has none. */
    fun tryResolveDispatchReceiver(): ExpEmbedding?

    /**
     * Returns the extension receiver embedding if [labelName] matches this resolver's label,
     * or null otherwise. The label check is necessary because nested lambdas each have their
     * own extension receiver, scoped to their label.
     */
    fun tryResolveExtensionReceiver(labelName: String): ExpEmbedding?

    /** Returns all formal parameter [VariableEmbedding]s, used e.g. when building Viper formals. */
    fun retrieveAllParams(): Sequence<VariableEmbedding>

    /**
     * The label identifying this function scope (used for named `return@label` resolution
     * and scoped extension receiver lookup). Null for anonymous or top-level scopes.
     */
    val labelName: String?

    /** The [ReturnTarget] that an unlabelled `return` in this scope should jump to. */
    val defaultResolvedReturnTarget: ReturnTarget
}

/**
 * Resolves a named return target for this resolver.
 *
 * Returns `defaultResolvedReturnTarget` if [returnPointName] matches [ParameterResolver.labelName],
 * or null if the label belongs to an outer scope (which will be handled by a parent resolver).
 */
fun ParameterResolver.resolveNamedReturnTarget(returnPointName: String): ReturnTarget? =
    (returnPointName == labelName).ifTrue { defaultResolvedReturnTarget }

/**
 * Standard [ParameterResolver] for a named (non-inlined) function body.
 *
 * Builds a map from each FIR value-parameter symbol to its corresponding [ExpEmbedding] from the
 * [FunctionSignature], so that parameter references in the body can be resolved in O(1).
 *
 * @param ctx The surrounding [ProgramConversionContext], available to sub-converters if needed.
 * @param signature The function's signature, providing formal params and receivers.
 * @param firArgs The FIR symbols for each value parameter, in declaration order.
 * @param labelName The label for this function scope (used for `return@label` and extension receiver lookup).
 * @param defaultResolvedReturnTarget The [ReturnTarget] for unlabelled returns.
 */
class RootParameterResolver(
    val ctx: ProgramConversionContext,
    private val signature: FunctionSignature,
    firArgs: List<FirValueParameterSymbol>,
    override val labelName: String?,
    override val defaultResolvedReturnTarget: ReturnTarget,
) : ParameterResolver {
    // Zip FIR symbols with their embedding counterparts from the signature, for fast lookup.
    private val parameters = firArgs.zip(signature.params).toMap()

    override fun tryResolveParameter(symbol: FirValueParameterSymbol): ExpEmbedding? = parameters[symbol]
    override fun tryResolveDispatchReceiver() = signature.dispatchReceiver
    override fun tryResolveExtensionReceiver(labelName: String) = (labelName == this.labelName).ifTrue {
        signature.extensionReceiver
    }

    /** Yields all formal value parameters from the signature. */
    override fun retrieveAllParams(): Sequence<VariableEmbedding> = sequence {
        yieldAll(signature.params)
    }
}

/**
 * Context that describes a single substitution: replace occurrences of [substitutionSymbol]
 * with [variable]. Used to map the `result` pseudo-parameter in postconditions to the actual
 * Viper variable holding the return value.
 */
interface ReturnVarSubstitutionContext {
    /** Returns the substituted variable if [symbol] matches the substitution symbol, otherwise null. */
    fun resolve(symbol: FirValueParameterSymbol): VariableEmbedding?
}

/**
 * Concrete [ReturnVarSubstitutionContext] that substitutes exactly one symbol.
 *
 * @param substitutionSymbol The FIR symbol representing `result` in the contract spec.
 * @param variable The concrete `VariableEmbedding` to substitute in its place.
 */
class ReturnVarSubstitutor(val substitutionSymbol: FirValueParameterSymbol, val variable: VariableEmbedding) :
    ReturnVarSubstitutionContext {
    override fun resolve(symbol: FirValueParameterSymbol) = (symbol == substitutionSymbol).ifTrue { variable }
}

/**
 * A [ParameterResolver] that wraps a [RootParameterResolver] and intercepts lookups for one
 * specific symbol — the return variable used in postconditions.
 *
 * All other resolution (dispatch receiver, extension receiver, other params, label, return target)
 * is delegated to [rootResolver] unchanged via the `by` delegation.
 *
 * @param rootResolver The underlying resolver for the function being specified.
 * @param substitutionContext Provides the concrete [VariableEmbedding] for the `result` symbol.
 */
class SubstitutedReturnParameterResolver(
    private val rootResolver: RootParameterResolver,
    private val substitutionContext: ReturnVarSubstitutionContext,
) : ParameterResolver by rootResolver {
    /** Checks the substitution first; falls back to the root resolver for all other parameters. */
    override fun tryResolveParameter(symbol: FirValueParameterSymbol): ExpEmbedding? =
        substitutionContext.resolve(symbol) ?: rootResolver.tryResolveParameter(symbol)
}

/**
 * Sealed hierarchy representing the three kinds of arguments that can be substituted when
 * inlining a function call: a value parameter, the extension receiver (`this` of extension),
 * or the dispatch receiver (`this` of method).
 *
 * Wrapper class: in inline functions we want to substitute actual parameters with our own anonymous variables with unique names.
 */
sealed interface SubstitutedArgument {
    /** A regular value parameter, identified by its FIR symbol. */
    data class ValueParameter(val symbol: FirValueParameterSymbol) : SubstitutedArgument

    /** The extension receiver (`this` in an extension function or lambda-with-receiver). */
    data object ExtensionThis : SubstitutedArgument

    /** The dispatch receiver (`this` in a member function). */
    data object DispatchThis : SubstitutedArgument
}

/**
 * [ParameterResolver] used when inlining a function call at a call site.
 *
 * Instead of looking up parameters from a [FunctionSignature], it uses a caller-supplied
 * [substitutions] map from [SubstitutedArgument] keys to the actual argument [ExpEmbedding]s.
 * This ensures that each inlined call site uses fresh, uniquely-named anonymous variables
 * rather than the callee's original parameter names, avoiding name collisions when the
 * same function is inlined multiple times.
 *
 * @param substitutions Maps each formal argument kind to the caller-provided expression.
 * @param labelName The label for this inlined scope.
 * @param defaultResolvedReturnTarget The return target for unlabelled returns inside the inlined body.
 */
class InlineParameterResolver(
    private val substitutions: Map<SubstitutedArgument, ExpEmbedding>,
    override val labelName: String?,
    override val defaultResolvedReturnTarget: ReturnTarget,
) : ParameterResolver {
    override fun tryResolveParameter(symbol: FirValueParameterSymbol): ExpEmbedding? =
        substitutions[SubstitutedArgument.ValueParameter(symbol)]

    override fun tryResolveDispatchReceiver() = substitutions[SubstitutedArgument.DispatchThis]
    override fun tryResolveExtensionReceiver(labelName: String) = (labelName == this.labelName).ifTrue {
        substitutions[SubstitutedArgument.ExtensionThis]
    }

    /**
     * Returns all substituted argument expressions as [VariableEmbedding]s.
     * The `!!` is safe here because inline arguments are always concrete variables
     * (not arbitrary expressions) at the point this is called.
     */
    override fun retrieveAllParams(): Sequence<VariableEmbedding> = sequence {
        yieldAll(substitutions.values.asSequence().map { it.underlyingVariable!! })
    }
}