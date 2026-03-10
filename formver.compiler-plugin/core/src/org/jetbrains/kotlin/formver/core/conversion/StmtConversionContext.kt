/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.isBoolean
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.embeddings.FunctionBodyEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.LabelEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.callables.FullNamedFunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.properties.ClassPropertyAccess
import org.jetbrains.kotlin.formver.core.embeddings.properties.PropertyAccessEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.properties.asPropertyAccess
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.isCustom
import org.jetbrains.kotlin.formver.core.isInvariantBuilderFunctionNamed
import org.jetbrains.kotlin.formver.core.linearization.Linearizer
import org.jetbrains.kotlin.formver.core.linearization.PureLinearizer
import org.jetbrains.kotlin.formver.core.linearization.SeqnBuilder
import org.jetbrains.kotlin.formver.core.linearization.SharedLinearizationState
import org.jetbrains.kotlin.formver.core.purity.checkValidity
import org.jetbrains.kotlin.formver.core.purity.isPure
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

/**
 * Interface for statement conversion.
 *
 * Extends [MethodConversionContext] with the mutable, statement-level state needed while
 * walking a FIR function body: the current `when`-subject variable, safe-call subject caching,
 * active exception catch labels, and loop/scope management.
 *
 * All conversion of FIR statements and expressions flows through [convert], which dispatches
 * to [StmtConversionVisitor].
 *
 * Naming convention:
 * - Functions that return a new `StmtConversionContext` should describe what change they make (`addResult`, `removeResult`...)
 * - Functions that take a lambda to execute should describe what extra state the lambda will have (`withResult`...)
 */
interface StmtConversionContext : MethodConversionContext {

    /**
     * The temporary variable holding the subject of the current `when` expression, if any.
     * Each branch of the `when` compares against this variable instead of re-evaluating the subject.
     */
    val whenSubject: VariableEmbedding?

    /**
     * In a safe call `callSubject?.foo()` we evaluate the call subject first to check for nullness.
     * In case it is not null, we evaluate the call to `callSubject.foo()`. Here we don't want to evaluate
     * the `callSubject` again so we store it in the `StmtConversionContext`.
     */
    val checkedSafeCallSubject: ExpEmbedding?

    /** The stack of active exception catch labels for the current try-catch nesting. */
    val activeCatchLabels: List<LabelEmbedding>

    /** Returns the Viper label name used as the `continue` target for the given (optional) loop label. */
    fun continueLabelName(targetName: String? = null): SymbolicName

    /** Returns the Viper label name used as the `break` target for the given (optional) loop label. */
    fun breakLabelName(targetName: String? = null): SymbolicName

    /** Registers the Kotlin loop label [targetName] with the current while-loop index. */
    fun addLoopName(targetName: String)

    /**
     * Converts a single FIR statement or expression into an [ExpEmbedding].
     * This is the main dispatch point; it delegates to [StmtConversionVisitor].
     */
    fun convert(stmt: FirStatement): ExpEmbedding

    /**
     * Executes [action] in a new inner variable scope.
     * Local variables declared inside [action] are invisible outside it.
     */
    fun <R> withNewScope(action: StmtConversionContext.() -> R): R

    /**
     * Executes [action] in a scope that prohibits local variable creation (e.g. a `forall` block).
     * This is used to enforce that quantifier bodies contain only pure expressions.
     */
    fun <R> withNoScope(action: StmtConversionContext.() -> R): R

    /**
     * Executes [action] in a new method conversion context created by [factory].
     * Used when entering an inlined function body or a lambda body that requires its own
     * parameter resolver and scope, without changing the outer context.
     */
    fun <R> withMethodCtx(factory: MethodContextFactory, action: StmtConversionContext.() -> R): R

    /**
     * Executes [action] in a context that has a fresh while-loop index bound to [label].
     * The loop index is used to generate unique `continue`/`break` label names.
     */
    fun <R> withFreshWhile(label: FirLabel?, action: StmtConversionContext.() -> R): R

    /**
     * Executes [action] with [subject] set as the active `when`-subject variable.
     * Pass null to clear the subject (e.g. outside a `when` expression).
     */
    fun <R> withWhenSubject(subject: VariableEmbedding?, action: StmtConversionContext.() -> R): R

    /**
     * Executes [action] with [subject] stored as the safe-call subject.
     * Avoids re-evaluating the receiver of a `?.` call when checking for null and invoking the method.
     */
    fun <R> withCheckedSafeCallSubject(subject: ExpEmbedding?, action: StmtConversionContext.() -> R): R

    /**
     * Registers [catches] as the active exception handlers, then executes [action].
     * Returns the [CatchBlockListData] (Viper catch label info) alongside the action's result,
     * so the caller can wire up the catch branches after converting the try body.
     */
    fun <R> withCatches(
        catches: List<FirCatch>,
        action: StmtConversionContext.(catchBlockListData: CatchBlockListData) -> R,
    ): Pair<CatchBlockListData, R>
}

/**
 * Registers a local property in this context and returns a [Declare] embedding that
 * allocates the backing variable with an optional [initializer].
 */
fun StmtConversionContext.declareLocalProperty(symbol: FirPropertySymbol, initializer: ExpEmbedding?): Declare {
    registerLocalProperty(symbol)
    return Declare(embedLocalProperty(symbol), initializer)
}

/**
 * Registers a local variable (e.g. from a destructuring or for-loop) and returns a
 * [Declare] embedding for it with an optional [initializer].
 */
fun StmtConversionContext.declareLocalVariable(symbol: FirVariableSymbol<*>, initializer: ExpEmbedding?): Declare {
    registerLocalVariable(symbol)
    return Declare(embedLocalVariable(symbol), initializer)
}

/**
 * Creates a fresh anonymous variable of the given [type] and returns a [Declare] embedding
 * with an optional [initializer]. Used for temporaries that have no corresponding Kotlin symbol.
 */
fun StmtConversionContext.declareAnonVar(type: TypeEmbedding, initializer: ExpEmbedding?): Declare =
    Declare(freshAnonVar(type), initializer)


/**
 * Returns the non-fake property symbols that are valid intersections of this override symbol —
 * i.e. those with the same `isVal` flag as the override itself.
 */
val FirIntersectionOverridePropertySymbol.propertyIntersections
    get() = intersections.filterIsInstanceAnd<FirPropertySymbol> { it.isVal == isVal }

/**
 * Tries to find final property symbol actually declared in some class instead of
 * (potentially) fake property symbol.
 * Note that if some property is found it is fixed since
 * 1. there can't be two non-abstract properties which don't subsume each other
 * in the hierarchy (kotlin disallows that) and final properties can't be abstract;
 * 2. final property can't subsume other final property as that means final property
 * is overridden.
 * //TODO: decide if we leave this lookup or consider it unsafe.
 *
 * For a regular (non-intersection-override) symbol, returns `this` if it is final and not
 * custom, or null otherwise. For an [FirIntersectionOverridePropertySymbol], recursively
 * searches its intersections for the first final, non-custom property.
 */
fun FirPropertySymbol.findFinalParentProperty(): FirPropertySymbol? =
    if (this !is FirIntersectionOverridePropertySymbol)
        (isFinal && !isCustom).ifTrue { this }
    else propertyIntersections.firstNotNullOfOrNull { it.findFinalParentProperty() }


/**
 * This is a key function when looking up properties.
 * It translates a kotlin `receiver.field` expression to an `ExpEmbedding`.
 *
 * Note that in FIR this `field` may be represented as `FirIntersectionOverridePropertySymbol`
 * which is necessary when the property could hypothetically inherit from multiple sources.
 * However, we don't register such symbols in the context when traversing the class.
 * Hence, some advanced logic is needed here.
 *
 * First, we try to find an actual backing field somewhere in the parents of the field with a
 * dfs-like algorithm on `FirIntersectionOverridePropertySymbol`s (it also should be final).
 *
 * If final backing field is not found, we lazily create a getter/setter pair for this
 * `FirIntersectionOverrideProperty`.
 *
 * Three cases are handled based on the call-site symbol and receiver:
 * - [FirValueParameterSymbol]: treat the parameter directly as a property access.
 * - [FirPropertySymbol] with a dispatch receiver: look up the final backing field (if any)
 *   and wrap as a [ClassPropertyAccess] on the converted receiver.
 * - [FirPropertySymbol] with an extension receiver: always uses a custom getter/setter via the receiver.
 * - [FirPropertySymbol] with no receiver: treat as a local property access.
 */
fun StmtConversionContext.embedPropertyAccess(accessExpression: FirPropertyAccessExpression): PropertyAccessEmbedding =
    when (val calleeSymbol = accessExpression.calleeReference.symbol) {
        is FirValueParameterSymbol -> embedParameter(calleeSymbol).asPropertyAccess()
        is FirPropertySymbol -> {
            val type = embedType(calleeSymbol.resolvedReturnType)
            when {
                accessExpression.dispatchReceiver != null -> {
                    val property = calleeSymbol.findFinalParentProperty()?.let {
                        embedProperty(it)
                    } ?: embedProperty(calleeSymbol)
                    ClassPropertyAccess(convert(accessExpression.dispatchReceiver!!), property, type)
                }

                accessExpression.extensionReceiver != null -> {
                    val property = embedProperty(calleeSymbol)
                    ClassPropertyAccess(convert(accessExpression.extensionReceiver!!), property, type)
                }

                else -> embedLocalProperty(calleeSymbol)
            }
        }

        else ->
            error("Property access symbol $calleeSymbol has unsupported type.")
    }


/**
 * Prepares a single call argument for use at an inline call site.
 *
 * Lambda arguments are passed through as-is (they will be inlined by the caller).
 * For all other arguments, type invariants are attached, and if the result is not already
 * a plain variable, a fresh anonymous variable is declared to hold the value so it is only
 * evaluated once.
 *
 * Returns a [Pair] of an optional [Declare] (null if no temporary is needed) and the
 * [ExpEmbedding] to use as the argument at the call site.
 */
fun StmtConversionContext.argumentDeclaration(
    arg: ExpEmbedding,
    callType: TypeEmbedding
): Pair<Declare?, ExpEmbedding> =
    when (arg.ignoringMetaNodes()) {
        is LambdaExp -> null to arg
        else -> {
            val argWithInvariants = arg.withNewTypeInvariants(callType) {
                proven = true
                access = true
            }
            // If `argWithInvariants` is `Cast(...(Cast(someVariable))...)` it is fine to use it
            // since in Viper it will always be translated to `someVariable`.
            // On other hand, `TypeEmbedding` and invariants in Viper are guaranteed
            // via previous line.
            if (argWithInvariants.underlyingVariable != null) null to argWithInvariants
            else declareAnonVar(callType, argWithInvariants).let {
                it to it.variable
            }
        }
    }

/**
 * Prepares all arguments for an inline function call by calling [argumentDeclaration] for each.
 *
 * Returns:
 * - A list of [Declare] nodes for any temporaries that were introduced.
 * - A list of argument [ExpEmbedding]s to substitute for the callee's formal parameters.
 */
fun StmtConversionContext.getInlineFunctionCallArgs(
    args: List<ExpEmbedding>,
    formalArgTypes: List<TypeEmbedding>,
): Pair<List<Declare>, List<ExpEmbedding>> {
    val declarations = mutableListOf<Declare>()
    val storedArgs = args.zip(formalArgTypes).map { (arg, callType) ->
        argumentDeclaration(arg, callType).let { (declaration, usage) ->
            declarations.addIfNotNull(declaration)
            usage
        }
    }
    return Pair(declarations, storedArgs)
}

/**
 * Emits the Viper embedding for an inlined function call.
 *
 * Inlining works by:
 * 1. Allocating a fresh [ReturnTarget] variable to capture the inlined function's return value.
 * 2. Preparing argument temporaries via [getInlineFunctionCallArgs].
 * 3. Building an [InlineParameterResolver] that maps each formal parameter to the corresponding argument.
 * 4. Converting the callee's FIR body in a new [MethodConversionContext] (with optional [parentCtx]
 *    for lambda captures) so that the inlined body's symbol references resolve correctly.
 * 5. Wrapping the result in a [Block] that declares the return variable, the argument temporaries,
 *    the inlined body expression, and a unit-invariant guard on the return variable.
 *
 * @param calleeSignature The signature of the function being inlined.
 * @param paramNames The [SubstitutedArgument] keys identifying each formal parameter.
 * @param args The actual argument expressions at the call site.
 * @param body The FIR body block of the function being inlined.
 * @param returnTargetName The label name for `return` statements inside the inlined body.
 * @param parentCtx An optional parent context for variable capture (e.g. lambda over outer locals).
 */
fun StmtConversionContext.insertInlineFunctionCall(
    calleeSignature: FunctionSignature,
    paramNames: List<SubstitutedArgument>,
    args: List<ExpEmbedding>,
    body: FirBlock,
    returnTargetName: String?,
    parentCtx: MethodConversionContext? = null,
): ExpEmbedding {
    // TODO: It seems like it may be possible to avoid creating a local here, but it is not clear how.
    val returnTarget = returnTargetProducer.getFresh(calleeSignature.callableType.returnType)
    val (declarations, callArgs) = getInlineFunctionCallArgs(args, calleeSignature.callableType.formalArgTypes)
    val subs = paramNames.zip(callArgs).toMap()
    val methodCtxFactory = MethodContextFactory(
        calleeSignature,
        InlineParameterResolver(subs, returnTargetName, returnTarget),
        parent = parentCtx,
    )
    return withMethodCtx(methodCtxFactory) {
        Block {
            add(Declare(returnTarget.variable, null))
            addAll(declarations)
            add(FunctionExp(null, convert(body), returnTarget.label))
            // if unit is what we return we might not guarantee it yet
            add(returnTarget.variable.withIsUnitInvariantIfUnit())
        }
    }
}

/**
 * Insert `ForAllEmbedding` where `forAll` function call was encountered.
 *
 * Translates a `forAll { x -> ... }` call into a Viper `forall` quantifier.
 * A fresh builtin (non-ref) anonymous variable is created for the bound variable [symbol],
 * and the lambda body [block] is converted in a no-scope context (disallowing local declarations)
 * with an [InlineParameterResolver] that maps [symbol] to the anonymous variable.
 *
 * The block may contain both invariant expressions and `triggers()` calls; both are extracted
 * via [collectInvariantsAndTriggers] and assembled into a [ForAllEmbedding].
 */
fun StmtConversionContext.insertForAllFunctionCall(
    symbol: FirValueParameterSymbol,
    block: FirBlock,
): ExpEmbedding {
    val anonVar = freshAnonBuiltinVar(embedType(symbol.resolvedReturnType))
    val methodCtxFactory = MethodContextFactory(
        signature,
        InlineParameterResolver(
            substitutions = mapOf(SubstitutedArgument.ValueParameter(symbol) to anonVar),
            labelName = null,
            // TODO: ideally, there shouldn't be a return target since return is prohibited
            defaultResolvedReturnTarget = defaultResolvedReturnTarget,
        ),
        parent = this,
    )
    return withNoScope {
        withMethodCtx(methodCtxFactory) {
            val (invariants, triggers) = collectInvariantsAndTriggers(block)
            ForAllEmbedding(anonVar, invariants, triggers)
        }
    }
}

/**
 * Converts a non-pure (regular) function body to a [FunctionBodyEmbedding].
 *
 * Steps:
 * 1. Converts the FIR body to an [ExpEmbedding] via `convert`.
 * 2. Wraps it in a [FunctionExp] tied to the [returnTarget] label, so that `return` statements
 *    jump to the correct target.
 * 3. Runs the [Linearizer] to flatten the embedding into a Viper `Seqn` (statement sequence).
 * 4. Appends a unit-invariant guard for the return variable (in case no `return` was encountered).
 * 5. Runs [checkValidity] to validate the embedding tree and collect any errors.
 *
 * Returns null if the function has no body (e.g. abstract or external functions).
 */
fun StmtConversionContext.convertMethodWithBody(
    declaration: FirSimpleFunction,
    signature: FullNamedFunctionSignature,
    returnTarget: ReturnTarget,
): FunctionBodyEmbedding? {
    val firBody = declaration.body ?: return null
    val body = convert(firBody)
    val bodyExp = FunctionExp(signature, body, returnTarget.label)
    val seqnBuilder = SeqnBuilder(declaration.source)
    val linearizer = Linearizer(SharedLinearizationState(anonVarProducer), seqnBuilder, declaration.source)
    bodyExp.toViperUnusedResult(linearizer)
    // note: we must guarantee somewhere that returned value is Unit
    // as we may not encounter any `return` statement in the body
    returnTarget.variable.withIsUnitInvariantIfUnit().toViperUnusedResult(linearizer)

    // TODO: Stop translation if this check fails
    body.checkValidity(declaration.source, errorCollector)

    return FunctionBodyEmbedding(seqnBuilder.block, returnTarget, bodyExp)
}

/**
 * Converts a `@Pure` function body to a single Viper expression ([Exp]).
 *
 * Steps:
 * 1. Requires a non-null FIR body (pure functions must have a visible body).
 * 2. Converts the FIR body to an [ExpEmbedding] via `convert`.
 * 3. Asserts the embedding is pure via [isPure]; throws [SnaktInternalException] if not.
 * 4. Runs [PureLinearizer] (which forbids statement generation) to reduce the embedding
 *    to a single Viper expression.
 * 5. Returns the resulting [Exp], which becomes the body of a Viper `function` declaration.
 */
fun StmtConversionContext.convertFunctionWithBody(
    declaration: FirSimpleFunction
): Exp {
    val firBody = declaration.body ?: throw SnaktInternalException(
        declaration.source,
        "Pure functions expect a function body to exist"
    )
    val body = convert(firBody)
    if (!body.isPure()) throw SnaktInternalException(
        declaration.source,
        "Impure function body detected in pure function"
    )
    val pureLinearizer = PureLinearizer(declaration.source)
    body.toViperUnusedResult(pureLinearizer)
    return pureLinearizer.constructExpression()
}

// Error message emitted when a non-boolean or non-expression statement appears in an invariant block.
private const val INVALID_STATEMENT_MSG =
    "Every statement in invariant block must be a pure boolean invariant."

/**
 * Holds the result of parsing a `forAll { ... }` or loop-invariant block that may contain
 * both invariant expressions and quantifier trigger expressions.
 */
data class InvariantsAndTriggers(
    val invariants: List<ExpEmbedding>,
    val triggers: List<ExpEmbedding>
)

/**
 * Converts a [FirBlock] used as a specification clause (e.g. `preconditions { }`,
 * `postconditions { }`, or `loopInvariants { }`) into a list of [ExpEmbedding]s.
 *
 * Every statement in the block must be a boolean expression; non-boolean statements
 * or non-expression statements cause a hard error via [INVALID_STATEMENT_MSG].
 */
fun StmtConversionContext.collectInvariants(block: FirBlock) = buildList {
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
 *
 * The `triggers()` call must have a single varargs argument; each element of the vararg
 * becomes a Viper quantifier trigger expression.
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

/**
 * Converts a [FirBlock] that may contain both invariant expressions and `triggers()` calls
 * into an [InvariantsAndTriggers] result.
 *
 * Each statement is first checked with [tryExtractTriggers]; if it is a `triggers()` call,
 * its arguments are added to the trigger list. Otherwise it is validated as a boolean expression
 * and added to the invariant list.
 *
 * Used for `forall { }` blocks where Viper quantifier triggers can be specified alongside invariants.
 */
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