/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.formver.common.ErrorCollector
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.common.UnsupportedFeatureBehaviour
import org.jetbrains.kotlin.formver.core.*
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.callables.*
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.properties.*
import org.jetbrains.kotlin.formver.core.embeddings.types.*
import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.names.SimpleNameResolver
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Program
import org.jetbrains.kotlin.formver.viper.debugMangled
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

/**
 * Central conversion context for an entire Viper program derived from a single Kotlin function
 * under analysis. It owns all deduplicated embeddings (methods, pure functions, classes,
 * properties, fields) and is the single source of truth for the Viper [Program] that will
 * eventually be handed to Silicon for verification.
 *
 * Every global entity (type, callable, field) must be embedded through this context so that
 * duplicate embeddings are collapsed into one — e.g. calling [embedClass] twice for the same
 * class symbol always yields the same [ClassTypeEmbedding] instance.
 *
 * We need the [FirSession] to resolve types and look up annotations.
 */
class ProgramConverter(
    val session: FirSession,
    override val config: PluginConfiguration,
    override val errorCollector: ErrorCollector
) :
    ProgramConversionContext {

    // ── Embedding registries ──────────────────────────────────────────────────

    // Viper *methods* (impure callables): pre-populated with hardcoded stdlib embeddings
    // (SpecialKotlinFunctions) and partially-special ones (e.g. String.plus).
    // User-defined non-pure functions are added on demand via embedFunction / embedUserFunction.
    private val methods: MutableMap<SymbolicName, FunctionEmbedding> =
        buildMap {
            putAll(SpecialKotlinFunctions.byName)
            putAll(PartiallySpecialKotlinFunctions.generateAllByName())
        }.toMutableMap()

    // Viper *functions* (pure callables): user-defined @Pure functions added via embedPureFunction.
    private val functions: MutableMap<SymbolicName, PureFunctionEmbedding> = mutableMapOf()

    // All class types seen so far; used to build the RuntimeTypeDomain for the Viper program.
    private val classes: MutableMap<SymbolicName, ClassTypeEmbedding> = mutableMapOf()

    // Property embeddings (getter + optional setter) for all properties referenced in the body.
    private val properties: MutableMap<SymbolicName, PropertyEmbedding> = mutableMapOf()

    // Viper fields collected from all processed class backing fields.
    private val fields: MutableSet<FieldEmbedding> = mutableSetOf()

    // ── Debug support ─────────────────────────────────────────────────────────

    // Returns a snapshot of the body expression embeddings for all user-defined methods,
    // keyed by their symbolic name. Used when @DumpExpEmbeddings is present on the function.
    // Cast is valid since we check that values are not null. We specify the type for `filterValues` explicitly to ensure there's no
    // loss of type information earlier.
    @Suppress("UNCHECKED_CAST")
    val debugExpEmbeddings: Map<SymbolicName, ExpEmbedding>
        get() = methods
            .mapValues { (it.value as? UserFunctionEmbedding)?.body?.debugExpEmbedding }
            .filterValues { value: ExpEmbedding? -> value != null } as Map<SymbolicName, ExpEmbedding>

    // ── Fresh-entity producers ────────────────────────────────────────────────

    // Produce unique indices/names for while-loops, catch labels, try-exit labels, and scopes.
    override val whileIndexProducer = indexProducer()
    override val catchLabelNameProducer = simpleFreshEntityProducer(::CatchLabelName)
    override val tryExitLabelNameProducer = simpleFreshEntityProducer(::TryExitLabelName)
    override val scopeIndexProducer = scopeIndexProducer()

    // The type annotation is necessary for the code to compile.
    // Producers for anonymous Viper variables (ref-typed and builtin-typed) and return targets.
    override val anonVarProducer = FreshEntityProducer(::AnonymousVariableEmbedding)
    override val anonBuiltinVarProducer = FreshEntityProducer(::AnonymousBuiltinVariableEmbedding)
    override val returnTargetProducer = FreshEntityProducer(::ReturnTarget)

    // Resolves symbolic names to human-readable strings for debug output and error messages.
    override val nameResolver = SimpleNameResolver()

    // ── Program assembly ──────────────────────────────────────────────────────

    /**
     * Assembles the complete Viper [Program] from all registered embeddings.
     * Called after [registerForVerification] has populated the registries.
     *
     * - domains: the runtime-type domain derived from all embedded classes.
     * - fields: special built-in fields plus all user-defined backing fields (deduplicated by name).
     * - functions: built-in pure functions plus user @Pure functions (deduplicated).
     * - methods: built-in methods plus user non-pure functions (deduplicated).
     * - predicates: shared/unique access predicates for every embedded class.
     */
    val program: Program
        get() = Program(
            domains = listOf(RuntimeTypeDomain(classes.values.toList())),
            // We need to deduplicate fields since public fields with the same name are represented differently
            // at `FieldEmbedding` level but map to the same Viper.
            fields = SpecialFields.all.map { it.toViper() } +
                    fields.distinctBy { it.name.debugMangled }.map { it.toViper() },
            functions = SpecialFunctions.all +
                    functions.values.mapNotNull { it.viperFunction }.distinctBy { it.name.debugMangled },
            methods = SpecialMethods.all +
                    methods.values.mapNotNull { it.viperMethod }.distinctBy { it.name.debugMangled },
            predicates = classes.values.flatMap {
                listOf(
                    it.details.sharedPredicate,
                    it.details.uniquePredicate
                )
            },
        )

    // ── Entry point ───────────────────────────────────────────────────────────

    /**
     * Main entry point called by `ViperPoweredDeclarationChecker` for each function under analysis.
     *
     * 1. Builds the full function signature (parameter types, pre/postconditions, name).
     * 2. Creates a [MethodConverter] and its [StmtConversionContext] for body conversion.
     * 3. Routes to the pure or impure path based on the @Pure annotation:
     *    - @Pure  → [embedPureUserFunction] + [StmtConversionContext.convertFunctionWithBody]
     *               produces a Viper *function* with an expression body.
     *    - non-pure → [embedUserFunction] + [StmtConversionContext.convertMethodWithBody]
     *                 produces a Viper *method* with a statement body.
     *
     * The embedding is registered in the map *before* the body is assigned to allow
     * mutually recursive functions to find each other's signatures.
     */
    fun registerForVerification(declaration: FirSimpleFunction) {
        val signature = embedFullSignature(declaration.symbol)
        val returnTarget = returnTargetProducer.getFresh(signature.callableType.returnType)
        val paramResolver =
            RootParameterResolver(
                this@ProgramConverter,
                signature,
                signature.symbol.valueParameterSymbols,
                signature.labelName,
                returnTarget,
            )
        val stmtCtx =
            MethodConverter(
                this@ProgramConverter,
                signature,
                paramResolver,
                scopeIndexProducer.getFresh(),
            ).statementCtxt()

        // Note: it is important that `body` is only set after `embedUserFunction` is complete, as we need to
        // place the embedding in the map before processing the body.
        if (declaration.symbol.isPure(session)) {
            embedPureUserFunction(declaration.symbol, signature).apply {
                body = stmtCtx.convertFunctionWithBody(declaration)
            }
        } else {
            embedUserFunction(declaration.symbol, signature).apply {
                body = stmtCtx.convertMethodWithBody(declaration, signature, returnTarget)
            }
        }
    }

    // ── Callable embedding ────────────────────────────────────────────────────

    /**
     * Returns (or creates) the [PureUserFunctionEmbedding] for a @Pure user function.
     * The body is left unset here and assigned by the caller after this returns,
     * so that the entry exists in [functions] before recursive calls are resolved.
     */
    fun embedPureUserFunction(
        symbol: FirFunctionSymbol<*>,
        signature: FullNamedFunctionSignature
    ): PureUserFunctionEmbedding {
        (functions[signature.name] as? PureUserFunctionEmbedding)?.also { return it }
        val new = PureUserFunctionEmbedding(processCallable(symbol, signature))
        functions[signature.name] = new
        return new
    }

    /**
     * Returns (or creates) the [UserFunctionEmbedding] for a non-pure user function.
     * Body is assigned by the caller after registration (same deferred pattern as above).
     */
    fun embedUserFunction(symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature): UserFunctionEmbedding {
        (methods[signature.name] as? UserFunctionEmbedding)?.also { return it }
        val new = UserFunctionEmbedding(processCallable(symbol, signature))
        methods[signature.name] = new
        return new
    }

    /**
     * Returns (or creates) a [FunctionEmbedding] for the getter of a property.
     * Getters are always treated as impure methods (they may access heap state).
     */
    private fun embedGetterFunction(symbol: FirPropertySymbol): FunctionEmbedding {
        val name = symbol.embedGetterName(this)
        return methods.getOrPut(name) {
            val signature = GetterFunctionSignature(name, symbol)
            UserFunctionEmbedding(
                NonInlineNamedFunction(signature)
            )
        }
    }

    /**
     * Returns (or creates) a [FunctionEmbedding] for the setter of a property.
     */
    private fun embedSetterFunction(symbol: FirPropertySymbol): FunctionEmbedding {
        val name = symbol.embedSetterName(this)
        return methods.getOrPut(name) {
            val signature = SetterFunctionSignature(name, symbol)
            UserFunctionEmbedding(
                NonInlineNamedFunction(signature)
            )
        }
    }

    /**
     * Looks up or creates the [FunctionEmbedding] for any non-pure callable.
     *
     * Handles three cases:
     * - Not seen before: create a fresh [UserFunctionEmbedding].
     * - Already a [PartiallySpecialKotlinFunction] without a base: inject the user embedding as base.
     * - Any other existing entry: return it as-is (e.g. fully special stdlib functions).
     */
    override fun embedFunction(symbol: FirFunctionSymbol<*>): FunctionEmbedding {
        val lookupName = symbol.embedName(this)
        return when (val existing = methods[lookupName]) {
            null -> {
                val signature = embedFullSignature(symbol)
                val callable = processCallable(symbol, signature)
                UserFunctionEmbedding(callable).also {
                    methods[lookupName] = it
                }
            }

            is PartiallySpecialKotlinFunction -> {
                if (existing.baseEmbedding != null)
                    return existing
                val signature = embedFullSignature(symbol)
                val callable = processCallable(symbol, signature)
                val userFunction = UserFunctionEmbedding(callable)
                existing.also {
                    it.initBaseEmbedding(userFunction)
                }
            }

            else -> existing
        }
    }

    /**
     * Looks up or creates the [PureFunctionEmbedding] for a @Pure callable.
     * Used when a @Pure function is referenced from another function's body or specification.
     */
    override fun embedPureFunction(symbol: FirFunctionSymbol<*>): PureFunctionEmbedding {
        val lookupName = symbol.embedName(this)
        return when (val existing = functions[lookupName]) {
            null -> {
                val signature = embedFullSignature(symbol)
                val callable = processCallable(symbol, signature)
                PureUserFunctionEmbedding(callable).also {
                    functions[lookupName] = it
                }
            }

            else -> existing
        }
    }

    /**
     * Dispatches to [embedPureFunction] or [embedFunction] based on the @Pure annotation.
     * Used when a call site doesn't know upfront whether the callee is pure.
     */
    override fun embedAnyFunction(symbol: FirFunctionSymbol<*>): CallableEmbedding =
        if (symbol.isPure(session)) {
            embedPureFunction(symbol)
        } else {
            embedFunction(symbol)
        }

    // ── Class embedding ───────────────────────────────────────────────────────

    /**
     * Returns an embedding of the class type, with details set.
     *
     * Class embedding is split into four phases to avoid circular dependencies
     * (a class may reference itself through its own properties):
     * 1. Register a name-only placeholder in [classes] to break cycles.
     * 2. Initialise supertypes (recursively embedding each supertype's class).
     * 3. Initialise fields from the class's property backing fields.
     * 4. Process property embeddings (getter/setter) for each property.
     */
    private fun embedClass(symbol: FirRegularClassSymbol): ClassTypeEmbedding {
        val className = symbol.classId.embedName()
        val embedding = classes.getOrPut(className) {
            buildClassPretype {
                withName(className)
            }
        }
        if (embedding.hasDetails) return embedding

        val newDetails =
            ClassEmbeddingDetails(
                embedding,
                symbol.classKind.isInterface,
            )
        embedding.initDetails(newDetails)

        // The full class embedding is necessary to process the signatures of the properties of the class, since
        // these take the class as a parameter. We thus do this in three phases:
        // 1. Initialise the supertypes (including running this whole four-step process on each)
        // 2. Initialise the fields
        // 3. Process the properties of the class.
        //
        // With respect to the embedding, each phase is pure by itself, and only updates the class embedding at the end.
        // This ensures the code never sees half-built supertype or field data. The phases can, however, modify the
        // `ProgramConverter`.

        // Phase 1: embed each supertype class (may recurse) then register the resolved list.
        newDetails.initSuperTypes(symbol.resolvedSuperTypes.map { embedType(it).pretype })

        // Phase 2: collect backing fields for non-special properties; register field→name pairs.
        val properties = symbol.propertySymbols
        newDetails.initFields(properties.mapNotNull { propertySymbol ->
            SpecialProperties.isSpecial(propertySymbol).ifFalse {
                processBackingField(propertySymbol, symbol)
            }
        }.toMap())

        // Phase 3: build getter/setter embeddings for each property.
        properties.forEach { processProperty(it, newDetails) }

        return embedding
    }

    // ── Type embedding ────────────────────────────────────────────────────────

    /** Converts a [ConeKotlinType] to a [TypeEmbedding], embedding any referenced classes as a side-effect. */
    override fun embedType(type: ConeKotlinType): TypeEmbedding = buildType { embedTypeWithBuilder(type) }

    // Note: keep in mind that this function is necessary to resolve the name of the function!
    /** Converts a function symbol's type signature into a [FunctionTypeEmbedding]. */
    override fun embedFunctionPretype(symbol: FirFunctionSymbol<*>): FunctionTypeEmbedding = buildFunctionPretype {
        embedFunctionPretypeWithBuilder(symbol)
    }

    // ── Property embedding ────────────────────────────────────────────────────

    /**
     * Returns the [PropertyEmbedding] for a given property symbol.
     *
     * - Extension properties are always embedded as custom properties (no backing field).
     * - Member properties: the containing class is embedded first (to register its fields),
     *   then the property is looked up in [properties]. If it isn't there (intersection override),
     *   it is embedded as a custom property.
     */
    override fun embedProperty(symbol: FirPropertySymbol): PropertyEmbedding = if (symbol.isExtension) {
        embedCustomProperty(symbol)
    } else {
        // Ensure that the class has been processed.
        embedType(symbol.dispatchReceiverType!!)
        properties.getOrPut(symbol.embedMemberPropertyName()) {
            check(symbol is FirIntersectionOverridePropertySymbol) {
                "Unknown property ${symbol.callableId}."
            }
            embedCustomProperty(symbol)
        }
    }

    // ── Constructor parameter helpers ─────────────────────────────────────────

    /**
     * Runs [action] with the primary-constructor value parameter that corresponds to this property,
     * or returns null if no such parameter exists.
     */
    private fun <R> FirPropertySymbol.withConstructorParam(action: FirPropertySymbol.(FirValueParameterSymbol) -> R): R? =
        correspondingValueParameterFromPrimaryConstructor?.let { param ->
            action(param)
        }

    /**
     * For primary constructors, maps each value parameter to the [FieldEmbedding] of the
     * corresponding backing field in the constructed class. Returns an empty map for
     * non-primary or non-constructor callables.
     *
     * Used to generate postconditions asserting that constructor parameters equal the
     * resulting object's fields (see [embedFullSignature]).
     */
    private fun extractConstructorParamsAsFields(symbol: FirFunctionSymbol<*>): Map<FirValueParameterSymbol, FieldEmbedding> {
        if (symbol !is FirConstructorSymbol || !symbol.isPrimary)
            return emptyMap()
        val constructedClassSymbol = symbol.resolvedReturnType.toRegularClassSymbol(session) ?: return emptyMap()
        val constructedClass = embedClass(constructedClassSymbol)

        return constructedClassSymbol.propertySymbols.mapNotNull { propertySymbol ->
            propertySymbol.withConstructorParam { paramSymbol ->
                constructedClass.details.findField(callableId.embedUnscopedPropertyName())?.let { paramSymbol to it }
            }
        }.toMap()
    }

    // ── Signature construction ────────────────────────────────────────────────

    /**
     * Builds the base [FunctionSignature] for a callable: dispatch/extension receivers,
     * value parameters (with uniqueness/borrowed annotations), and return type.
     */
    override fun embedFunctionSignature(symbol: FirFunctionSymbol<*>): FunctionSignature {
        val dispatchReceiverType = symbol.receiverType
        val extensionReceiverType = symbol.extensionReceiverType
        val isExtensionReceiverUnique = symbol.receiverParameterSymbol?.isUnique(session) ?: false
        val isExtensionReceiverBorrowed = symbol.receiverParameterSymbol?.isBorrowed(session) ?: false
        return object : FunctionSignature {
            override val callableType: FunctionTypeEmbedding = embedFunctionPretype(symbol)

            // TODO: figure out whether we want a symbol here and how to get it.
            override val dispatchReceiver = dispatchReceiverType?.let {
                PlaceholderVariableEmbedding(
                    DispatchReceiverName,
                    embedType(it),
                    isUnique = false,
                    isBorrowed = false,
                )
            }

            override val extensionReceiver = extensionReceiverType?.let {
                PlaceholderVariableEmbedding(
                    ExtensionReceiverName,
                    embedType(it),
                    isExtensionReceiverUnique,
                    isExtensionReceiverBorrowed,
                )
            }

            override val params = symbol.valueParameterSymbols.map {
                FirVariableEmbedding(
                    it.embedName(),
                    embedType(it.resolvedReturnType),
                    it,
                    it.isUnique(session),
                    it.isBorrowed(session)
                )
            }
        }
    }

    /** Collects all [FirPropertySymbol]s declared (or overridden) in this class. */
    @OptIn(SymbolInternals::class)
    private val FirRegularClassSymbol.propertySymbols: List<FirPropertySymbol>
        get() {
            val result = mutableListOf<FirPropertySymbol>()
            this.fir.processAllDeclarations(session) {
                if (it is FirPropertySymbol) result.add(it)
            }
            return result
        }

    /**
     * Builds the [FullNamedFunctionSignature] for a callable, which extends the base signature with:
     * - A stable symbolic name (used as Viper identifier).
     * - Pre- and postconditions derived from:
     *   - Type invariants and access permissions for each parameter.
     *   - Standard library contracts (e.g. String length non-negative).
     *   - Explicit @Pre / @Post annotations (via [ContractDescriptionConversionVisitor]).
     *   - Kotlin `contract { }` blocks (via [extractFirSpecification]).
     * - For primary constructors: field-equality postconditions tying constructor params to fields.
     *
     * Note: specifications are restricted to [FirSimpleFunction]s with a visible body; functions
     * from other modules without a body cannot have their specs embedded.
     */
    private fun embedFullSignature(symbol: FirFunctionSymbol<*>): FullNamedFunctionSignature {
        val subSignature = object : NamedFunctionSignature, FunctionSignature by embedFunctionSignature(symbol) {
            override val name = symbol.embedName(this@ProgramConverter)
            override val labelName: String
                get() = super<NamedFunctionSignature>.labelName
            override val symbol = symbol
        }
        val constructorParamSymbolsToFields = extractConstructorParamsAsFields(symbol)
        val contractVisitor = ContractDescriptionConversionVisitor(this@ProgramConverter, subSignature)

        @OptIn(SymbolInternals::class)
        val declaration = symbol.fir
        val body = declaration.body

        /** Specifications are only allowed inside simple functions.
         * We are also unable to retrieve them when body is not visible,
         * although ideally we should be able to see preconditions and postconditions
         * from other modules.
         */
        val firSpec = when {
            declaration !is FirSimpleFunction -> null
            body == null -> null
            else -> extractFirSpecification(body, declaration.symbol.resolvedReturnType)
        }

        // A shared root resolver for embedding specifications; uses a dummy return target
        // (depth 0) since specs are not inside the function body.
        val rootResolver =
            RootParameterResolver(
                this@ProgramConverter,
                subSignature,
                subSignature.symbol.valueParameterSymbols,
                subSignature.labelName,
                ReturnTarget(depth = 0, subSignature.callableType.returnType),
            )

        // Helper: create a fresh StmtConversionContext for embedding a spec clause,
        // optionally substituting the return variable (needed for postconditions).
        fun createCtx(returnVariable: VariableEmbedding? = null): StmtConversionContext {
            val returnVarCtx = returnVariable?.let { ret -> firSpec?.returnVar?.let { ReturnVarSubstitutor(it, ret) } }
            val paramResolver =
                if (returnVarCtx != null)
                    SubstitutedReturnParameterResolver(rootResolver, returnVarCtx)
                else
                    rootResolver

            return MethodConverter(
                this@ProgramConverter,
                subSignature,
                paramResolver,
                scopeDepth = ScopeIndex.NoScope,
            ).statementCtxt()
        }

        return object : FullNamedFunctionSignature, NamedFunctionSignature by subSignature {
            /**
             * Preconditions for this function in Viper:
             * - Pure invariants and access permissions for each formal argument.
             * - Unique predicate access for @Unique parameters.
             * - Standard library preconditions (e.g. index bounds for stdlib calls).
             * - User-written @Pre / contract preconditions.
             */
            // TODO (inhale vs require) Decide if `predicateAccessInvariant` should be required rather than inhaled in the beginning of the body.
            override fun getPreconditions() = buildList {
                subSignature.formalArgs.forEach {
                    addAll(it.pureInvariants())
                    addAll(it.accessInvariants())
//                    // For pure functions (Viper `function`), parameter type info cannot be inhaled
//                    // as a statement — it must come from `requires`. Non-pure functions get this
//                    // via `inhale` in FunctionExp, so we skip it there to avoid redundant preconditions.
//                    if (subSignature.symbol.isPure(session)) {
//                        addAll(it.provenInvariants())
//                    }
                    if (it.isUnique) {
                        addIfNotNull(it.type.uniquePredicateAccessInvariant()?.fillHole(it))
                    }
                }
                addAll(subSignature.stdLibPreconditions())
                // We create a separate context to embed the preconditions here.
                // Hence, some parts of FIR are translated later than the other and less explicitly.
                // TODO: this process should be a separate step in the conversion.
                firSpec?.precond?.let {
                    addAll(createCtx().collectInvariants(it))
                }
            }

            /**
             * Postconditions for this function in Viper:
             * - Access permissions for each formal argument (after the call).
             * - Unique predicate access for @Unique borrowed parameters.
             * - Pure invariants and proven invariants on the return variable.
             * - For non-pure functions: access invariants and unique predicate for the return value.
             * - Contract-based postconditions (@Post / Kotlin contracts).
             * - Standard library postconditions.
             * - For primary constructors: field-equality assertions.
             * - User-written @Post / contract postconditions.
             */
            override fun getPostconditions(returnVariable: VariableEmbedding) = buildList {
                subSignature.formalArgs.forEach {
                    addAll(it.accessInvariants())
                    if (it.isUnique && it.isBorrowed) {
                        addIfNotNull(it.type.uniquePredicateAccessInvariant()?.fillHole(it))
                    }
                }
                addAll(returnVariable.pureInvariants())
                addAll(returnVariable.provenInvariants())
                if (!subSignature.symbol.isPure(session)) {
                    addAll(returnVariable.allAccessInvariants())
                    if (subSignature.callableType.returnsUnique) {
                        addIfNotNull(returnVariable.uniquePredicateAccessInvariant())
                    }
                }
                addAll(contractVisitor.getPostconditions(ContractVisitorContext(returnVariable, symbol)))
                addAll(subSignature.stdLibPostconditions(returnVariable))
                addIfNotNull(primaryConstructorInvariants(returnVariable))
                // TODO: this process should be a separate step in the conversion (see above)
                firSpec?.postcond?.let {
                    addAll(createCtx(returnVariable).collectInvariants(it))
                }
            }

            /**
             * For primary constructors: emits a conjunction asserting that each constructor
             * parameter equals the corresponding field of the newly created object.
             * Returns null if no such field-parameter pairs exist.
             */
            fun primaryConstructorInvariants(returnVariable: VariableEmbedding): ExpEmbedding? {
                val invariants = params.mapNotNull { param ->
                    require(param is FirVariableEmbedding) { "Constructor parameters must be represented by FirVariableEmbeddings" }
                    constructorParamSymbolsToFields[param.symbol]?.let { field ->
                        (field.accessPolicy == AccessPolicy.ALWAYS_READABLE).ifTrue {
                            EqCmp(FieldAccess(returnVariable, field), param)
                        }
                    }
                }
                return if (invariants.isEmpty()) null
                else invariants.toConjunction()
            }

            override val declarationSource: KtSourceElement? = symbol.source
        }
    }

    // ── Receiver type helpers ─────────────────────────────────────────────────

    /** For property accessors, returns the property symbol; for all others, returns self. */
    private val FirFunctionSymbol<*>.containingPropertyOrSelf
        get() = when (this) {
            is FirPropertyAccessorSymbol -> propertySymbol
            else -> this
        }

    /** The dispatch receiver type of this callable (or its owning property). */
    private val FirFunctionSymbol<*>.receiverType: ConeKotlinType?
        get() = containingPropertyOrSelf.dispatchReceiverType

    /** The extension receiver type of this callable (or its owning property). */
    private val FirFunctionSymbol<*>.extensionReceiverType: ConeKotlinType?
        get() = containingPropertyOrSelf.resolvedReceiverTypeRef?.coneType

    // ── Field and property processing ─────────────────────────────────────────

    /**
     * Construct and register the field embedding for this property's backing field, if any exists.
     *
     * A backing field is created when the property:
     * - Has a backing field (no custom getter that bypasses it).
     * - Is not a specially-handled property (e.g. String.length).
     * - Is final (or its class is final), ensuring the field value cannot be overridden.
     *
     * Returns a [SimpleKotlinName] → [FieldEmbedding] pair for registration in the class details,
     * or null if no backing field should be created.
     */
    private fun processBackingField(
        symbol: FirPropertySymbol,
        classSymbol: FirRegularClassSymbol,
    ): Pair<SimpleKotlinName, FieldEmbedding>? {
        val embedding = embedClass(classSymbol)
        val unscopedName = symbol.callableId.embedUnscopedPropertyName()
        val scopedName = symbol.callableId.embedMemberBackingFieldName(
            Visibilities.isPrivate(symbol.visibility)
        )
        val fieldIsAllowed = symbol.hasBackingField
                && !symbol.isCustom
                && (symbol.isFinal || classSymbol.isFinal)
        val backingField = scopedName.specialEmbedding(embedding) ?: fieldIsAllowed.ifTrue {
            UserFieldEmbedding(
                scopedName,
                embedType(symbol.resolvedReturnType),
                symbol,
                symbol.isUnique(session),
                embedding,
                symbol.isManual(session)
            )
        }
        return backingField?.let { unscopedName to it }
    }

    /**
     * Construct and register the property embedding (i.e. getter + setter) for this property.
     *
     * Note that the property either has associated Viper field (and then it is used to access the value) or not (in this case methods are used).
     * The field is only used for final properties with default getter and default setter (if any).
     *
     * Null value of parameter [embedding] means that there is no class details corresponding to this type (e.g. it is primitive).
     */
    private fun processProperty(symbol: FirPropertySymbol, embedding: ClassEmbeddingDetails?) {
        val unscopedName = symbol.callableId.embedUnscopedPropertyName()
        properties[symbol.embedMemberPropertyName()] =
            SpecialProperties.byCallableId[symbol.callableId] ?: embedding.run {
                val backingField = embedding?.findField(unscopedName)
                backingField?.let { fields.add(it) }
                embedProperty(symbol, backingField)
            }
    }

    /** Embeds a property that has no backing field (extension or intersection override). */
    private fun embedCustomProperty(symbol: FirPropertySymbol) = embedProperty(symbol, null)

    /** Builds a [PropertyEmbedding] from a getter and optional setter embedding. */
    private fun embedProperty(symbol: FirPropertySymbol, backingField: FieldEmbedding?) =
        PropertyEmbedding(
            embedGetter(symbol, backingField),
            symbol.isVar.ifTrue { embedSetter(symbol, backingField) },
        )

    /**
     * Returns a [GetterEmbedding]:
     * - [BackingFieldGetter] if a Viper field backs this property (direct field read).
     * - [CustomGetter] otherwise (calls the getter Viper method).
     */
    private fun embedGetter(symbol: FirPropertySymbol, backingField: FieldEmbedding?): GetterEmbedding =
        if (backingField != null) {
            BackingFieldGetter(backingField)
        } else {
            CustomGetter(embedGetterFunction(symbol))
        }

    /**
     * Returns a [SetterEmbedding]:
     * - [BackingFieldSetter] if a Viper field backs this property (direct field write).
     * - [CustomSetter] otherwise (calls the setter Viper method).
     */
    private fun embedSetter(symbol: FirPropertySymbol, backingField: FieldEmbedding?): SetterEmbedding =
        if (backingField != null) {
            BackingFieldSetter(backingField)
        } else {
            CustomSetter(embedSetterFunction(symbol))
        }

    // ── Callable body processing ──────────────────────────────────────────────

    /**
     * Builds the [RichCallableEmbedding] for a callable (without setting a body).
     *
     * - Inlineable functions (e.g. lambdas whose body is expanded at call sites) become
     *   [InlineNamedFunction] with the raw FIR body attached.
     * - All others become [NonInlineNamedFunction]. A dummy Viper header (function or method)
     *   is emitted immediately to ensure all referenced types are processed before the body
     *   is converted — otherwise types referenced only in contracts would be seen too late.
     */
    @OptIn(SymbolInternals::class)
    private fun processCallable(
        symbol: FirFunctionSymbol<*>,
        signature: FullNamedFunctionSignature
    ): RichCallableEmbedding {
        return if (symbol.shouldBeInlined) {
            InlineNamedFunction(signature, symbol.fir.body!!)
        } else {
            // We generate a dummy method header here to ensure all required types are processed already. If we skip this, any types
            // that are used only in contracts cause an error because they are not processed until too late.
            // TODO: fit this into the flow in some logical way instead.
            // TODO: We should emit the function with its body here instead of creating an empty header for functions
            NonInlineNamedFunction(
                signature,
                symbol.isPure(session)
            ).also { if (symbol.isPure(session)) it.toViperFunctionHeader() else it.toViperMethodHeader() }
        }
    }

    // ── Type builder helpers ──────────────────────────────────────────────────

    /**
     * Recursive [TypeBuilder] dispatch: maps a [ConeKotlinType] to its [PretypeBuilder].
     *
     * Handles the full Kotlin type hierarchy:
     * - Error types → hard error (should not appear after FIR resolution).
     * - Type parameters → embedded as nullable `Any` (over-approximation).
     * - Primitive types (Int, Boolean, Char, Unit, Nothing) → dedicated Viper builtins.
     * - String → special embedding that also processes String's property symbols.
     * - Function types → recursive embedding of receiver, params, and return type.
     * - Nullable types → sets the nullable flag and embeds the non-null variant.
     * - Regular classes → [embedClass] (which recursively embeds supertypes and fields).
     * - Unsupported types → [unimplementedTypeEmbedding] (throw or log based on config).
     */
    private fun TypeBuilder.embedTypeWithBuilder(type: ConeKotlinType): PretypeBuilder = when {
        type is ConeErrorType -> error("Encountered an erroneous type: $type")
        type is ConeTypeParameterType -> {
            isNullable = true; any()
        }

        type.isString -> {
            val stringClassSymbol = type.toClassSymbol(session) as FirRegularClassSymbol
            stringClassSymbol.propertySymbols.forEach {
                processProperty(it, embedding = null)
            }
            string()
        }

        type.isUnit -> unit()
        type.isChar -> char()
        type.isInt -> int()
        type.isBoolean -> boolean()
        type.isNothing -> nothing()
        type.isSomeFunctionType(session) -> function {
            check(type is ConeClassLikeType) { "Expected a ConeClassLikeType for a function type, got $type" }
            type.receiverType(session)?.let { withDispatchReceiver { embedTypeWithBuilder(it) } }
            type.valueParameterTypesWithoutReceivers(session).forEach { param ->
                withParam { embedTypeWithBuilder(param) }
            }
            withReturnType { embedTypeWithBuilder(type.returnType(session)) }
        }

        type.canBeNull(session) -> {
            isNullable = true
            embedTypeWithBuilder(type.withNullability(false, session.typeContext))
        }

        type.isAny -> any()
        type is ConeClassLikeType -> {
            val classLikeSymbol = type.toClassSymbol(session)
            if (classLikeSymbol is FirRegularClassSymbol) {
                existing(embedClass(classLikeSymbol))
            } else {
                unimplementedTypeEmbedding(type)
            }
        }

        else -> unimplementedTypeEmbedding(type)
    }

    /**
     * Populates a [FunctionPretypeBuilder] with the receiver, parameter, and return type
     * of the given function symbol. Also marks the return type as unique for constructors
     * and @Unique-annotated functions.
     */
    private fun FunctionPretypeBuilder.embedFunctionPretypeWithBuilder(symbol: FirFunctionSymbol<*>) {
        symbol.receiverType?.let {
            withDispatchReceiver { embedTypeWithBuilder(it) }
        }
        symbol.extensionReceiverType?.let {
            withExtensionReceiver { embedTypeWithBuilder(it) }
        }
        symbol.valueParameterSymbols.forEach { param ->
            withParam {
                embedTypeWithBuilder(param.resolvedReturnType)
            }
        }
        withReturnType { embedTypeWithBuilder(symbol.resolvedReturnType) }
        returnsUnique = symbol.isUnique(session) || symbol is FirConstructorSymbol
    }

    /**
     * Fallback for types that have no embedding yet. Behaviour depends on [config]:
     * - [UnsupportedFeatureBehaviour.THROW_EXCEPTION]: hard error (strict mode).
     * - [UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE]: log a minor error and substitute `Unit`
     *   so compilation can continue (lenient mode).
     */
    private fun TypeBuilder.unimplementedTypeEmbedding(type: ConeKotlinType): PretypeBuilder =
        when (config.behaviour) {
            UnsupportedFeatureBehaviour.THROW_EXCEPTION ->
                throw NotImplementedError("The embedding for type $type is not yet implemented.")

            UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE -> {
                errorCollector.addMinorError("Requested type $type, for which we do not yet have an embedding.")
                unit()
            }
        }
}