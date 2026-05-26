/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.isPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.common.UnsupportedFeatureBehaviour
import org.jetbrains.kotlin.formver.core.*
import org.jetbrains.kotlin.formver.core.diagnostics.ConversionErrors
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.callables.*
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.properties.*
import org.jetbrains.kotlin.formver.core.embeddings.types.*
import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.core.purity.checkValidity
import org.jetbrains.kotlin.formver.core.purity.isPure
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Program
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue


/**
 * Tracks the top-level information about the program.
 * Conversions for global entities like types should generally be
 * performed via this context to ensure they can be deduplicated.
 * We need the FirSession to get access to the TypeContext.
 */
class ProgramConverter(
    val session: FirSession,
    override val config: PluginConfiguration,
    private val diagnosticContext: DiagnosticContext,
    private val reporter: DiagnosticReporter,
) : ProgramConversionContext {

    /**
     * Whether any primary diagnostic has been reported during conversion.
     *
     * Excludes the derived [ConversionErrors.VERIFICATION_SKIPPED] summary, which is itself fired
     * in reaction to primary diagnostics.
     */
    var hadConversionError: Boolean = false
        private set

    /** Source attached to source-less diagnostics (e.g. unimplemented features raised deep in conversion). */
    private var currentDeclarationSource: KtSourceElement? = null

    private fun emit(source: KtSourceElement?, factory: KtDiagnosticFactory1<String>, msg: String) {
        context(diagnosticContext) {
            reporter.reportOn(source, factory, msg)
        }
        hadConversionError = true
    }

    override fun reportPurityViolation(source: KtSourceElement?, msg: String) =
        emit(source, ConversionErrors.PURITY_VIOLATION, msg)

    override fun reportMinorInternalError(msg: String) =
        emit(currentDeclarationSource, ConversionErrors.MINOR_INTERNAL_ERROR, msg)

    private fun reportVerificationSkipped(source: KtSourceElement?, msg: String) {
        context(diagnosticContext) {
            reporter.reportOn(source, ConversionErrors.VERIFICATION_SKIPPED, msg)
        }
    }

    private val specialFunctions: Map<SymbolicName, SpecialKotlinFunction> = buildMap {
        putAll(SpecialKotlinFunctions.byName)
        putAll(PartiallySpecialKotlinFunctions.generateAllByName())
    }

    private val fullSignatures: MutableMap<SymbolicName, FullNamedFunctionSignature> = mutableMapOf()

    private val callable: MutableMap<SymbolicName, SignatureWithTarget<CallableNamedSignature>> = mutableMapOf()

    private data class RegisteredFunction(
        val declaration: FirSimpleFunction,
        val signature: FullNamedFunctionSignature,
        val returnTarget: ReturnTarget,
    )

    private val registered: MutableList<RegisteredFunction> = mutableListOf()

    override val typeResolver: TypeResolver = TypeResolver()

    val debugExpEmbeddings: Map<SymbolicName, ExpEmbedding>
        get() = buildMap {
            convertedBodyResolver.forEachImpure { name, body -> put(name, body.bodyExp) }
        }


    override val whileIndexProducer = indexProducer()
    override val catchLabelNameProducer = simpleFreshEntityProducer(::CatchLabelName)
    override val tryExitLabelNameProducer = simpleFreshEntityProducer(::TryExitLabelName)
    override val scopeIndexProducer = scopeIndexProducer()

    // The type annotation is necessary for the code to compile.
    override val anonVarProducer = FreshEntityProducer(::AnonymousVariableEmbedding)
    override val anonBuiltinVarProducer = FreshEntityProducer(::AnonymousBuiltinVariableEmbedding)
    override val returnTargetProducer = FreshEntityProducer(ReturnTarget::createForDepth)
    override val nameResolver = ShortNameResolver()
    override val convertedBodyResolver = ConvertedBodyResolver()
    override val linearizedBodyResolver = LinearizedBodyResolver()


    fun buildProgram(): Program = Program(
        domains = listOf(RuntimeTypeDomain(typeResolver)),
        // Public fields with the same name are represented differently at `FieldEmbedding` level
        // but map to the same Viper field, so we deduplicate before emitting.
        fields = typeResolver.backingFields().distinctBy { it.name }.map { it.toViper() },
        functions = SpecialFunctions.all + linearizedBodyResolver.functions,
        methods = SpecialMethods.all + linearizedBodyResolver.methods,
        predicates = typeResolver.classTypeEmbeddings().map {
            with(typeResolver) {
                it.uniquePredicate()
            }
        },
        adts = emptyList(),
    )

    /**
     * Embed the declaration's signature and queue its body for later processing by [convertAll].
     */
    fun register(declaration: FirSimpleFunction) {
        val (signature, returnTarget) = embedCallable(declaration.symbol).refineSignature { current ->
            embedFullSignature(declaration.symbol, current.signature, current.returnTarget)
        }
        registered += RegisteredFunction(declaration, signature, returnTarget)
    }

    /**
     * Convert each registered declaration's body to an `ExpEmbedding`, populating [convertedBodyResolver].
     */
    fun convertAll() {
        for (entry in registered) {
            val (declaration, signature, returnTarget) = entry
            currentDeclarationSource = declaration.source
            val stmtCtx = createBodyConversionContext(signature, returnTarget)
            if (signature.isPure) {
                convertedBodyResolver.storePure(signature.name, stmtCtx.convertPureBody(declaration))
            } else {
                stmtCtx.convertImpureBody(declaration, signature, returnTarget)?.let {
                    convertedBodyResolver.storeImpure(signature.name, it)
                }
            }
        }
        currentDeclarationSource = null
    }

    /**
     * Walk converted bodies to surface validity / purity errors via the diagnostic reporter. If any
     * conversion error has been reported by the time this returns, emit a [ConversionErrors.VERIFICATION_SKIPPED]
     * summary on every registered declaration so the user sees per-function attribution for the bail-out.
     * Callers should inspect [hadConversionError] afterwards and skip [linearizeAll] when set.
     */
    fun validateAll() {
        convertedBodyResolver.forEachImpure { name, body ->
            val source = fullSignatures[name]?.declarationSource
            body.bodyExp.checkValidity(source, this)
        }
        convertedBodyResolver.forEachPure { name, body ->
            if (!body.isPure()) {
                val source = fullSignatures[name]?.declarationSource
                reportPurityViolation(source, "Impure function body detected in pure function")
            }
        }
        if (hadConversionError) {
            for (entry in registered) {
                reportVerificationSkipped(
                    entry.declaration.source,
                    "Function '${entry.declaration.name.asString()}' was not verified because of errors in its declaration",
                )
            }
        }
    }


    private fun linearizePure(name: SymbolicName, signature: FullNamedFunctionSignature) {
        val converted = convertedBodyResolver.lookupPure(name)
        val linearized = converted?.let { linearizePureBody(signature.declarationSource, it) }
        linearizedBodyResolver.storeFunction(name, signature.toViperFunction(typeResolver, linearized))

    }

    private fun linearizeImpure(name: SymbolicName, signature: FullNamedFunctionSignature) {
        val source = signature.declarationSource
        val converted = convertedBodyResolver.lookupImpure(name)
        val method = if (converted != null) {
            linearizeImpureBody(source, converted).toViperMethod(signature, typeResolver)
        } else {
            signature.toViperMethodHeader(typeResolver)
        }
        if (method != null) linearizedBodyResolver.storeMethod(name, method)

    }

    /**
     * Build the finalized Viper `Method` / `Function` for every embedded callable, storing each in
     * [linearizedBodyResolver]. Should only be invoked when [hadConversionError] is false.
     *
     * Iterates the embedding maps directly (rather than the converted-body map) so that callables
     * without a body still get a Viper header.
     */
    fun linearizeAll() {
        val (pure, impure) = fullSignatures.entries.partition { it.value.isPure }
        pure.forEach { linearizePure(it.key, it.value) }
        impure.forEach { linearizeImpure(it.key, it.value) }
    }

    private fun createBodyConversionContext(
        signature: NamedFunctionSignature, target: ReturnTarget
    ): StmtConversionContext {

        val paramResolver = RootParameterResolver(
            this@ProgramConverter, signature, signature.symbol.valueParameterSymbols, signature.labelName, target
        )
        val stmtCtx = MethodConverter(
            this@ProgramConverter,
            signature,
            paramResolver,
            scopeIndexProducer.getFresh(),
        ).statementCtxt()
        return stmtCtx
    }

    private fun createContractConversionContext(
        signature: NamedFunctionSignature,
        firSpec: FirSpecification,
        returnTarget: ReturnTarget,
    ): Pair<StmtConversionContext, StmtConversionContext> {

        val rootResolver = RootParameterResolver(
            this@ProgramConverter,
            signature,
            signature.symbol.valueParameterSymbols,
            signature.labelName,
            returnTarget,
        )


        val wrappedResolver = firSpec.returnVar?.let { ReturnVarSubstitutor(it, signature.returns) }
            ?.let { ctx -> SubstitutedReturnParameterResolver(rootResolver, ctx) } ?: rootResolver


        val preconditionContext = MethodConverter(
            this@ProgramConverter,
            signature,
            rootResolver,
            ScopeIndex.NoScope,
        ).statementCtxt()

        val postconditionContext = MethodConverter(
            this@ProgramConverter,
            signature,
            wrappedResolver,
            ScopeIndex.NoScope,
        ).statementCtxt()

        return Pair(preconditionContext, postconditionContext)
    }


    /**
     * Embeds the function signature of the specified symbol into a representation that includes
     * function type, receiver details, parameters, and return type.
     *
     * This can be used for any function (including inline, pure/impure, lambdas etc.)
     */
    override fun embedFunctionSignature(symbol: FirFunctionSymbol<*>): SignatureWithTarget<FunctionSignature> {
        val dispatchReceiverType = symbol.receiverType
        val extensionReceiverType = symbol.extensionReceiverType

        val returnType = embedType(symbol.resolvedReturnType)

        val returnTarget = when {
            symbol.isPure(session) -> ReturnTarget.createForPureFunction(returnType)
            else -> returnTargetProducer.getFresh(returnType)
        }

        val dispatchVariable = dispatchReceiverType?.let {
            PlaceholderVariableEmbedding(
                DispatchReceiverName,
                embedType(it),
                isUnique = false,
                isBorrowed = false,
            )
        }
        val extensionVariable = extensionReceiverType?.let {
            PlaceholderVariableEmbedding(
                ExtensionReceiverName,
                embedType(it),
                symbol.receiverParameterSymbol?.isUnique(session) ?: false,
                symbol.receiverParameterSymbol?.isBorrowed(session) ?: false,
            )
        }

        val parameterVariables = symbol.valueParameterSymbols.map {
            FirVariableEmbedding(
                it.embedName(), embedType(it.resolvedReturnType), it, it.isUnique(session), it.isBorrowed(session)
            )
        }

        val signature = FunctionSignatureImpl(
            embedFunctionPretype(symbol),
            dispatchVariable,
            extensionVariable,
            parameterVariables,
            returns = returnTarget.variable,
            isPure = symbol.isPure(session)
        )

        return SignatureWithTarget(signature, returnTarget)
    }

    /**
     * Embeds the function signature of the specific symbol. The symbol must have a name.
     */
    private fun embedNamedFunctionSignature(symbol: FirFunctionSymbol<*>): SignatureWithTarget<NamedFunctionSignature> =
        embedFunctionSignature(symbol).refineSignature { current ->
            val name = symbol.embedName(this)
            NamedFunctionSignatureImpl(name, symbol, current.signature)
        }


    /**
     * Embeds an inline signature. This also includes embedding the provided contract.

     * We want to be able to call the result. For an inline function to be callable, we need to have the pre- and post-conditions.
     * Therefore, the full signature is already registered here.
     */
    @OptIn(SymbolInternals::class)
    private fun embedInlineFunctionSignature(symbol: FirFunctionSymbol<*>): SignatureWithTarget<CallableNamedSignature> =
        embedNamedFunctionSignature(symbol).refineSignature { current ->
            val body =
                symbol.fir.body ?: throw SnaktInternalException(symbol.source, "Expected function body, got null")
            val (precondition, postcondition) = embedProvidedContract(symbol, current.signature, current.returnTarget)

            val fullSignature = InlineNamedFunction(
                current.signature,
                body,
                current.signature.basicPreconditions(typeResolver) + precondition,
                current.signature.userFunctionPostcondition(typeResolver) + postcondition
            )
            if (!symbol.neverConvert(session)) {
                fullSignatures.putIfAbsent(fullSignature.name, fullSignature)
            }
            fullSignature
        }


    /**
     * Embeds a non-inline function. The contract is not included.
     */
    private fun embedNonInlineFunctionSignature(symbol: FirFunctionSymbol<*>): SignatureWithTarget<CallableNamedSignature> =
        embedNamedFunctionSignature(symbol).refineSignature { current ->
            NonInlineNamedFunctionSignature(current.signature)
        }


    private fun embedCallable(symbol: FirFunctionSymbol<*>): SignatureWithTarget<CallableNamedSignature> {
        val name = symbol.embedName(this)
        return callable.getOrPut(name) {
            if (symbol.shouldBeInlined) {
                embedInlineFunctionSignature(symbol)
            } else {
                embedNonInlineFunctionSignature(symbol)
            }
        }
    }

    /**
     * Embeds and records full signature if it does not already exist.
     */
    private fun embedFullSignature(
        symbol: FirFunctionSymbol<*>, callable: CallableNamedSignature, returnTarget: ReturnTarget
    ): FullNamedFunctionSignature {
        val name = symbol.embedName(this)
        return fullSignatures.getOrPut(name) {
            when {
                symbol.isPrimaryConstructor() -> embedConstructorSignature(symbol, callable, returnTarget)
                else -> embedFullUserSignature(symbol, callable, returnTarget)
            }
        }
    }

    private fun embedConstructorSignature(
        symbol: FirConstructorSymbol, callable: CallableNamedSignature, returnTarget: ReturnTarget
    ): FullNamedFunctionSignature {
        val constructedClassSymbol =
            symbol.resolvedReturnType.toRegularClassSymbol(session) ?: throw SnaktInternalException(
                symbol.source, "Constructor does not return a regular class"
            )
        val parameterMatching = constructedClassSymbol.propertySymbols.mapNotNull { propertySymbol ->
            val name = propertySymbol.embedMemberPropertyName(this)
            propertySymbol.withConstructorParam { paramSymbol ->
                typeResolver.lookupDefaultBehavingProperties(name)?.let { paramSymbol to it }
            }
        }.toMap()

        val context = createBodyConversionContext(callable, returnTarget)

        val fieldPostconditions = callable.params.mapNotNull { param ->
            require(param is FirVariableEmbedding) { "Constructor parameters must be represented by FirVariableEmbeddings" }
            parameterMatching[param.symbol]?.let { property ->
                EqCmp(property.getter!!.getValueSimple(returnTarget.variable, context), param)
            }
        }
        return ConstructorSignature(callable, symbol, fieldPostconditions, typeResolver)
    }

    private fun embedFullUserSignature(
        symbol: FirFunctionSymbol<*>, callable: CallableNamedSignature, returnTarget: ReturnTarget
    ): FullNamedFunctionSignature {

        val (preconditions, postconditions) = embedProvidedContract(symbol, callable, returnTarget)
        return UserFunctionSignature(
            callable, symbol, preconditions, postconditions, typeResolver
        )
    }

    private fun embedImpureGetterFunction(symbol: FirPropertySymbol): NonInlineNamedFunction {
        val name = symbol.embedGetterName(this)
        val signature = ImpureGetterFunctionSignature(name, symbol)
        fullSignatures.putIfAbsent(name, signature)
        return signature
    }

    private fun embedPureGetterFunction(symbol: FirPropertySymbol): NonInlineNamedFunction {
        val name = symbol.embedGetterName(this)
        val classType = embedType(symbol.dispatchReceiverType!!)
        val returnType = embedType(symbol.resolvedReturnType)
        val signature = PureGetterFunctionSignature(name, symbol, classType, returnType)
        val callable = NonInlineNamedFunction(signature)
        val function = PureUserFunctionEmbedding(callable)
        fullSignatures.putIfAbsent(name, function)
        return callable
    }

    private fun embedSetterFunction(symbol: FirPropertySymbol): NonInlineNamedFunction {
        val name = symbol.embedSetterName(this)
        val signature = SetterFunctionSignature(name, symbol)
        val callable = NonInlineNamedFunction(signature)
        val function = UserFunctionEmbedding(callable)
        fullSignatures.putIfAbsent(name, function)
        return callable
    }


    /**
     * Embeds the full function signature (e.g. with pre+post conditions). If necessary, the body of the function is stored as well.
     */
    private fun embedFullFunctionAndBody(symbol: FirFunctionSymbol<*>): CallableNamedSignature {

        callable[symbol.embedName(this)]?.let { return it.signature }

        val (callableEmbedding, returnTarget) = embedCallable(symbol)
        // Inline functions, which should not be converted, don't need a signature.
        // The only thing we need is to be able to call them.
        if (symbol.neverConvert(session) && symbol.shouldBeInlined) {
            return callableEmbedding
        }
        embedFunctionBody(symbol, callableEmbedding, returnTarget)

        return callableEmbedding
    }

    /**
     * Embeds the contract of the provided callable. If necessary, also records the body.
     */
    @OptIn(SymbolInternals::class)
    private fun embedFunctionBody(
        symbol: FirFunctionSymbol<*>, callable: CallableNamedSignature, returnTarget: ReturnTarget
    ) {
        embedFullSignature(symbol, callable, returnTarget)
        if (callable.isPure && !symbol.neverConvert(session)) {
            val declaration = symbol.fir as? FirSimpleFunction ?: throw SnaktInternalException(
                symbol.source, "Expected FirSimpleFunction, got unexpected type ${symbol.fir.javaClass.simpleName}"
            )

            val context = createBodyConversionContext(callable, returnTarget)
            val body = context.convertPureBody(declaration)
            convertedBodyResolver.storePure(callable.name, body)
        }
    }


    /**
     * Embeds the full function signature (e.g. with pre+post conditions). If necessary, the body of the function is stored as well.
     */
    private fun embedFullFunctionAndBody(symbol: FirFunctionSymbol<*>): CallableNamedSignature {

        callable[symbol.embedName(this)]?.let { return it.signature }

        val (callableEmbedding, returnTarget) = embedCallable(symbol)


        embedFunctionBody(symbol, callableEmbedding, returnTarget)

        return callableEmbedding
    }

    /**
     * Returns the callable embedding if a matching special function exists.
     */
    fun embedSpecialFunction(symbol: FirFunctionSymbol<*>): CallableEmbedding? {
        val name = symbol.embedName(this)
        return specialFunctions[name]?.also { existing ->
            if (existing !is PartiallySpecialKotlinFunction) return@also
            if (existing.baseEmbedding != null) return@also
            val callable = embedFullFunctionAndBody(symbol)
            existing.initBaseEmbedding(callable)
        }
    }

    /**
     * This function is the public interface to embed a function symbol.
     * If during the conversion of a function body a function call is made, then [embedAnyFunction] must be used to embed the function.
     *
     * It will return the callable embedding, if the function was seen the first time, it will embed the full function signature and body (if needed).
     */
    override fun embedAnyFunction(symbol: FirFunctionSymbol<*>): CallableEmbedding {

        embedSpecialFunction(symbol)?.let { return it }

        return embedFullFunctionAndBody(symbol)
    }

    /**
     * Returns an embedding of the class type, with details set.
     */
    private fun embedClass(symbol: FirRegularClassSymbol): ClassTypeEmbedding {
        val className = symbol.classId.embedName()
        typeResolver.lookupClassTypeEmbedding(className)?.let { return it }

        val embedding = typeResolver.getEmbeddingOrExecute(className) {
            val classEmbedding = buildClassPretype {
                withName(className)
            }

            typeResolver.register(classEmbedding, symbol.classKind.isInterface)

            symbol.resolvedSuperTypes.forEach {
                val superTypeName = embedType(it).pretype.name
                typeResolver.addSubtypeRelation(className, superTypeName)
            }

            classEmbedding
        }
        symbol.propertySymbols.forEach {
            embedProperty(it)
        }
        return embedding
    }

    override fun embedType(type: ConeKotlinType): TypeEmbedding = buildType { embedTypeWithBuilder(type) }

    // Note: keep in mind that this function is necessary to resolve the name of the function!
    override fun embedFunctionPretype(symbol: FirFunctionSymbol<*>): FunctionTypeEmbedding = buildFunctionPretype {
        embedFunctionPretypeWithBuilder(symbol)
    }

    /**
     * This function embeds properties. Depending on the behavior of the property, it can be handled differently.
     *
     *
     * * Closed `val` (No Getter): Maps to a pure function. This can be done because the value cannot be changed.
     * * Closed `val` (With Getter): Maps to an impure function to execute the custom getter logic.
     * * Closed `var`: Maps to a backing field. This value needs permissions and can be changed.
     * * Open `val` / `var`: Maps to an impure method. Abstracted because subclasses can override the behavior, introduce side effects, or turn a `val` into a `var`.
     * * Extension properties are handled like open properties.
     */
    override fun embedProperty(symbol: FirPropertySymbol): PropertyEmbedding {

        if (symbol.receiverParameterSymbol != null) {
            return embedExtensionProperty(symbol)
        } else {
            val name = symbol.embedMemberPropertyName(this)
            return typeResolver.getOrPutProperty(name) {
                // Check if the symbol should receive a special treatment
                with(typeResolver) {
                    with(session) {
                        SpecialProperties.lookup(symbol)?.let { return@getOrPutProperty it }
                    }
                }

                val classSymbol = symbol.dispatchReceiverType?.toClassSymbol(session)

                val regularClass = classSymbol as? FirRegularClassSymbol ?: throw SnaktInternalException(
                    symbol.source, "Properties dispatch receiver is not a regular class"
                )

                val isDefaultProperty = isGuaranteedDefaultProperty(symbol)
                val isImmutable = symbol.isVal
                val isManual = symbol.isManual(session)
                val isUnique = symbol.isUnique(session)

                val type = embedType(symbol.resolvedReturnType)

                val (getter, setter) = when {
                    (isDefaultProperty && !isImmutable) || isManual -> {
                        // use a backing field
                        val field = embedBackingField(symbol, regularClass)
                        Pair(BackingFieldGetter(field), BackingFieldSetter(field))
                    }

                    isDefaultProperty -> {
                        // use pure function
                        Pair(
                            CustomGetter(embedPureGetterFunction(symbol)), null
                        )
                    }

                    else -> {
                        // The property could be overridden by anything, or has a custom getter/setter.
                        // use impure function
                        Pair(
                            CustomGetter(embedImpureGetterFunction(symbol)),
                            symbol.isVar.ifTrue { CustomSetter(embedSetterFunction(symbol)) })
                    }
                }

                return@getOrPutProperty PropertyEmbedding(
                    getter, setter, isDefaultProperty || isManual, isUnique, isImmutable, type
                )

            }
        }
    }

    private fun <R> FirPropertySymbol.withConstructorParam(action: FirPropertySymbol.(FirValueParameterSymbol) -> R): R? =
        correspondingValueParameterFromPrimaryConstructor?.let { param ->
            action(param)
        }


    @OptIn(DirectDeclarationsAccess::class)
    private val FirRegularClassSymbol.propertySymbols: List<FirPropertySymbol>
        get() = declarationSymbols.filterIsInstance<FirPropertySymbol>()


    private fun embedFormverContract(
        symbol: FirFunctionSymbol<*>, signature: NamedFunctionSignature, returnTarget: ReturnTarget
    ): Pair<List<ExpEmbedding>, List<ExpEmbedding>> {
        @OptIn(SymbolInternals::class) val declaration = symbol.fir
        val body = declaration.body

        /** Specifications are only allowed inside simple functions.
         * We are also unable to retrieve them when body is not visible,
         * although ideally we should be able to see preconditions and postconditions
         * from other modules.
         */
        if (declaration !is FirSimpleFunction || body == null) {
            return Pair(emptyList(), emptyList())
        }

        val firSpec = extractFirSpecification(body, declaration.symbol.resolvedReturnType)


        val (preconditionContext, postconditionContext) = createContractConversionContext(
            signature, firSpec, returnTarget
        )

        val preconditions = firSpec.precond?.let { preconditionContext.collectInvariants(it) } ?: emptyList()
        val postconditions = firSpec.postcond?.let { postconditionContext.collectInvariants(it) } ?: emptyList()

        return Pair(preconditions, postconditions)
    }

    private fun embedKotlinContract(
        signature: NamedFunctionSignature
    ): List<ExpEmbedding> {
        val contractVisitor = ContractDescriptionConversionVisitor(this@ProgramConverter, signature)
        return contractVisitor.getPostconditions()
    }

    private fun embedProvidedContract(
        symbol: FirFunctionSymbol<*>, signature: NamedFunctionSignature, returnTarget: ReturnTarget
    ): Pair<List<ExpEmbedding>, List<ExpEmbedding>> {
        val kotlinContractPostcondition = embedKotlinContract(signature)
        val userContract = embedFormverContract(symbol, signature, returnTarget)
        return Pair(userContract.first, kotlinContractPostcondition + userContract.second)
    }

    private val FirFunctionSymbol<*>.containingPropertyOrSelf
        get() = when (this) {
            is FirPropertyAccessorSymbol -> propertySymbol
            else -> this
        }

    private val FirFunctionSymbol<*>.receiverType: ConeKotlinType?
        get() = containingPropertyOrSelf.dispatchReceiverType

    private val FirFunctionSymbol<*>.extensionReceiverType: ConeKotlinType?
        get() = containingPropertyOrSelf.resolvedReceiverTypeRef?.coneType

    /**
     * Construct and register the field embedding for this property's backing field, if any exists.
     */
    private fun embedBackingField(
        symbol: FirPropertySymbol, classSymbol: FirRegularClassSymbol
    ): FieldEmbedding {
        val embedding = embedClass(classSymbol)
        val scopedName = symbol.callableId!!.embedMemberBackingFieldName(scopePolicy(symbol, this))
        val backingField = UserFieldEmbedding(
            scopedName,
            embedType(symbol.resolvedReturnType),
            symbol,
            symbol.isUnique(session),
            embedding,
            symbol.isManual(session)
        )
        return backingField
    }

    private fun embedExtensionProperty(symbol: FirPropertySymbol) = PropertyEmbedding(
        CustomGetter(embedImpureGetterFunction(symbol)),
        symbol.isVar.ifTrue { CustomSetter(embedSetterFunction(symbol)) },
        hasDefaultBehaviour = false,
        isUnique = false,
        isVal = symbol.isVal,
        type = embedType(symbol.resolvedReturnType)
    )


    private fun TypeBuilder.embedTypeWithBuilder(type: ConeKotlinType): PretypeBuilder = when {
        type is ConeErrorType -> error("Encountered an erroneous type: $type")
        type is ConeTypeParameterType -> {
            isNullable = true; any()
        }

        type.isString -> {
            val stringClassSymbol = type.toClassSymbol(session) as FirRegularClassSymbol
            stringClassSymbol.propertySymbols.forEach {
                embedProperty(it)
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

    private fun TypeBuilder.unimplementedTypeEmbedding(type: ConeKotlinType): PretypeBuilder = when (config.behaviour) {
        UnsupportedFeatureBehaviour.THROW_EXCEPTION -> throw NotImplementedError("The embedding for type $type is not yet implemented.")

        UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE -> {
            reportMinorInternalError("Requested type $type, for which we do not yet have an embedding.")
            unit()
        }
    }

    override fun isGuaranteedDefaultProperty(symbol: FirPropertySymbol): Boolean {
        val classSymbolFinal = symbol.dispatchReceiverType?.toClassSymbol(session)?.isFinal ?: false
        return (symbol.isFinal || classSymbolFinal) && !symbol.isCustom
    }
}
