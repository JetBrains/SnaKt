/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.isAny
import org.jetbrains.kotlin.formver.common.*
import org.jetbrains.kotlin.formver.core.*
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.callables.*
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.properties.*
import org.jetbrains.kotlin.formver.core.embeddings.types.*
import org.jetbrains.kotlin.formver.core.isAdt
import org.jetbrains.kotlin.formver.core.purity.checkValidity
import org.jetbrains.kotlin.formver.core.purity.isPure
import org.jetbrains.kotlin.formver.core.names.*
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
    override val errorCollector: ErrorCollector
) : ProgramConversionContext {
    private val methods: MutableMap<SymbolicName, CallableEmbedding> = buildMap {
        putAll(SpecialKotlinFunctions.byName)
        putAll(PartiallySpecialKotlinFunctions.generateAllByName())
    }.toMutableMap()
    private val functions: MutableMap<SymbolicName, PureUserFunctionEmbedding> = mutableMapOf()

    private val registered: MutableList<Triple<FirSimpleFunction, FullNamedFunctionSignature, ReturnTarget>> = mutableListOf()

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


    fun program(): Program = Program(
        domains = listOf(RuntimeTypeDomain(typeResolver)),
        // We need to deduplicate fields since public fields with the same name are represented differently
        // at `FieldEmbedding` level but map to the same Viper.
        fields = typeResolver.backingFields().distinctBy { it.name }
            .map { it.toViper() },
        functions = SpecialFunctions.all + functions.entries.mapNotNull { (name, embedding) ->
            val linearized = linearizedBodyResolver.lookupPure(name)
            check(linearized != null || convertedBodyResolver.lookupPure(name) == null) {
                "Internal error: pure function $name was converted but not linearized"
            }
            embedding.viperFunction(typeResolver, linearized)
        }.distinctBy { it.name },
        methods = SpecialMethods.all + methods.entries.mapNotNull { (name, embedding) ->
            val linearized = linearizedBodyResolver.lookupImpure(name)
            check(linearized != null || convertedBodyResolver.lookupImpure(name) == null) {
                "Internal error: impure function $name was converted but not linearized"
            }
            val callable = when (embedding) {
                is UserFunctionEmbedding -> embedding.callable
                is FullySpecialKotlinFunction -> null
                is PartiallySpecialKotlinFunction -> embedding.baseEmbedding?.callable
                else -> error("Unexpected embedding in methods map for $name: $embedding")
            } ?: return@mapNotNull null
            linearized?.toViperMethod(callable, typeResolver) ?: callable.toViperMethodHeader(typeResolver)
        }.distinctBy { it.name } + typeResolver.backingFields().map { it.type.havocMethod(typeResolver) }
            .distinctBy { it.name },
        predicates = typeResolver.classTypeEmbeddings().flatMap {
            with(typeResolver) {
                listOf(
                    it.sharedPredicate(), it.uniquePredicate()
                )
            }
        },
        adts = typeResolver.adtTypeEmbeddings().map { it.toViper() },
    )

    /**
     * Embed the declaration's signature and queue its body for later processing by [convertAll].
     */
    fun register(declaration: FirSimpleFunction) {
        val (returnTarget, signature) = embedFullSignature(declaration.symbol)
        if (declaration.symbol.isPure(session)) {
            ensurePureUserFunctionEmbedding(declaration.symbol, signature)
        } else {
            embedUserFunction(declaration.symbol, signature)
        }
        registered += Triple(declaration, signature, returnTarget)
    }

    /**
     * Convert each registered declaration's body to an `ExpEmbedding`, populating [convertedBodyResolver].
     * Pure callees reached transitively are converted via [embedPureFunction]; their entries land in the
     * same resolver.
     */
    fun convertAll() {
        for ((declaration, signature, returnTarget) in registered) {
            val stmtCtx = createBodyConversionContext(signature, returnTarget)
            if (declaration.symbol.isPure(session)) {
                convertedBodyResolver.storePure(signature.name, stmtCtx.convertPureBody(declaration))
            } else {
                stmtCtx.convertImpureBody(declaration, signature, returnTarget)?.let {
                    convertedBodyResolver.storeImpure(signature.name, it)
                }
            }
            if (errorCollector.collectedAdtError()) {
                errorCollector.addAdtError(
                    declaration.source,
                    "Function '${declaration.name.asString()}' references an invalid ADT",
                )
            }
        }
    }

    /**
     * Walk converted bodies to surface validity / purity errors via [errorCollector].
     * Bodies that fail are still present in [convertedBodyResolver]; callers are expected to inspect
     * [errorCollector] after this returns and bail before invoking [linearizeAll].
     */
    fun validateAll() {
        convertedBodyResolver.forEachImpure { name, body ->
            val source = (methods[name] as? UserFunctionEmbedding)?.callable?.declarationSource
            body.bodyExp.checkValidity(source, errorCollector)
        }
        convertedBodyResolver.forEachPure { name, body ->
            if (!body.isPure()) {
                val source = functions[name]?.callable?.declarationSource
                errorCollector.addPurityError(source, "Impure function body detected in pure function")
            }
        }
    }

    /**
     * Linearize each converted body to its Viper form, populating [linearizedBodyResolver].
     * Should only be invoked when [errorCollector] is clean.
     */
    fun linearizeAll() {
        convertedBodyResolver.forEachImpure { name, converted ->
            val source = (methods[name] as? UserFunctionEmbedding)?.callable?.declarationSource
            linearizedBodyResolver.storeImpure(name, linearizeImpureBody(source, converted))
        }
        convertedBodyResolver.forEachPure { name, body ->
            val source = functions[name]?.callable?.declarationSource
            linearizedBodyResolver.storePure(name, linearizePureBody(source, body))
        }
    }

    private fun ensurePureUserFunctionEmbedding(
        symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature
    ): PureUserFunctionEmbedding {
        (functions[signature.name] as? PureUserFunctionEmbedding)?.also { return it }
        val new = PureUserFunctionEmbedding(embedCallable(symbol, signature))
        functions[signature.name] = new
        return new
    }

    @OptIn(SymbolInternals::class)
    private fun embedPureUserFunction(
        symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature, returnTarget: ReturnTarget
    ): PureUserFunctionEmbedding {
        (functions[signature.name] as? PureUserFunctionEmbedding)?.also { return it }
        val new = ensurePureUserFunctionEmbedding(symbol, signature)
        val declaration = symbol.fir as? FirSimpleFunction ?: throw SnaktInternalException(
            symbol.source, "Expected FirSimpleFunction, got unexpected type ${symbol.fir.javaClass.simpleName}"
        )
        if (declaration.body != null) {
            val stmtCtx = createBodyConversionContext(signature, returnTarget)
            convertedBodyResolver.storePure(signature.name, stmtCtx.convertPureBody(declaration))
        }
        return new
    }

    private fun createBodyConversionContext(
        signature: NamedFunctionSignature,
        target: ReturnTarget
    ): StmtConversionContext {

        val paramResolver = RootParameterResolver(
            this@ProgramConverter,
            signature,
            signature.symbol.valueParameterSymbols,
            signature.labelName,
            target
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


        val wrappedResolver = firSpec.returnVar
            ?.let { ReturnVarSubstitutor(it, signature.returns) }
            ?.let { ctx -> SubstitutedReturnParameterResolver(rootResolver, ctx) }
            ?: rootResolver


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

    fun embedUserFunction(
        symbol: FirFunctionSymbol<*>,
        signature: FullNamedFunctionSignature,
    ): UserFunctionEmbedding {
        val name = symbol.embedName(this)
        (methods[name] as? UserFunctionEmbedding)?.also { return it }
        val new = UserFunctionEmbedding(embedCallable(symbol, signature))
        methods[name] = new
        return new
    }

    private fun embedGetterFunction(symbol: FirPropertySymbol): CallableEmbedding {
        val name = symbol.embedGetterName(this)
        return methods.getOrPut(name) {
            val signature = GetterFunctionSignature(name, symbol)
            UserFunctionEmbedding(
                NonInlineNamedFunction(signature)
            )
        }
    }

    private fun embedSetterFunction(symbol: FirPropertySymbol): CallableEmbedding {
        val name = symbol.embedSetterName(this)
        return methods.getOrPut(name) {
            val signature = SetterFunctionSignature(name, symbol)
            UserFunctionEmbedding(
                NonInlineNamedFunction(signature)
            )
        }
    }

    override fun embedFunction(symbol: FirFunctionSymbol<*>): CallableEmbedding {
        val lookupName = symbol.embedName(this)
        return when (val existing = methods[lookupName]) {
            null -> {
                val callable = embedCallable(symbol)
                UserFunctionEmbedding(callable).also {
                    methods[lookupName] = it
                }
            }

            is PartiallySpecialKotlinFunction -> {
                if (existing.baseEmbedding != null) return existing
                val callable = embedCallable(symbol)
                val userFunction = UserFunctionEmbedding(callable)
                existing.also {
                    it.initBaseEmbedding(userFunction)
                }
            }

            else -> existing
        }
    }

    override fun embedPureFunction(symbol: FirFunctionSymbol<*>): PureUserFunctionEmbedding {
        val (returnTarget, signature) = embedFullSignature(symbol)
        return embedPureUserFunction(symbol, signature, returnTarget)
    }

    override fun embedAnyFunction(symbol: FirFunctionSymbol<*>): CallableEmbedding = if (symbol.isPure(session)) {
        embedPureFunction(symbol)
    } else {
        embedFunction(symbol)
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

    private fun embedAdtClass(symbol: FirRegularClassSymbol): AdtTypeEmbedding =
        typeResolver.getOrRegisterAdt(symbol.classId.embedName()) {
            if (!validateAdtHeader(symbol)) InvalidAdtTypeEmbedding
            else AdtTypeEmbeddingImpl(symbol.classId.embedName())
        }

    @OptIn(DirectDeclarationsAccess::class)
    private fun validateAdtHeader(symbol: FirRegularClassSymbol): Boolean {
        if (!symbol.classKind.isObject || !symbol.isData) {
            errorCollector.addAdtError(
                symbol.source, "Invalid ADT annotation: @ADT may only be applied to data object declarations"
            )
            return false
        }
        for (declaration in symbol.declarationSymbols) when (declaration) {
            is FirPropertySymbol -> {
                errorCollector.addAdtError(
                    declaration.source, "Invalid ADT annotation: An @ADT data object must not have fields"
                )
                return false
            }
            is FirNamedFunctionSymbol -> {
                errorCollector.addAdtError(
                    declaration.source, "Invalid ADT annotation: An @ADT data object must not have member functions"
                )
                return false
            }
        }
        if (symbol.resolvedSuperTypes.any { !it.isAny }) {
            errorCollector.addAdtError(
                symbol.source, "Invalid ADT annotation: An @ADT data object must not extend a class or implement an interface"
            )
            return false
        }
        return true
    }

    override fun embedType(type: ConeKotlinType): TypeEmbedding = buildType { embedTypeWithBuilder(type) }

    // Note: keep in mind that this function is necessary to resolve the name of the function!
    override fun embedFunctionPretype(symbol: FirFunctionSymbol<*>): FunctionTypeEmbedding = buildFunctionPretype {
        embedFunctionPretypeWithBuilder(symbol)
    }

    override fun embedProperty(symbol: FirPropertySymbol): PropertyEmbedding {

        if (symbol.receiverParameterSymbol != null) {
            return embedCustomProperty(symbol)
        } else {
            val name = symbol.embedMemberPropertyName()
            return typeResolver.getOrPutProperty(name) {
                // Check if the symbol should receive a special treatment
                with(typeResolver) {
                    with(session) {
                        SpecialProperties.lookup(symbol)?.let { return@getOrPutProperty it }
                    }
                }
                // Check if the symbol can be represented using a backing field
                embedBackingField(symbol)?.let {
                    return@getOrPutProperty PropertyEmbedding(
                        BackingFieldGetter(it), BackingFieldSetter(it)
                    )
                }
                // Create a custom getter+setter
                embedCustomProperty(symbol)
            }
        }
    }

    private fun <R> FirPropertySymbol.withConstructorParam(action: FirPropertySymbol.(FirValueParameterSymbol) -> R): R? =
        correspondingValueParameterFromPrimaryConstructor?.let { param ->
            action(param)
        }

    override fun embedFunctionSignature(symbol: FirFunctionSymbol<*>): Pair<ReturnTarget, FunctionSignature> {
        val dispatchReceiverType = symbol.receiverType
        val extensionReceiverType = symbol.extensionReceiverType
        val isExtensionReceiverUnique = symbol.receiverParameterSymbol?.isUnique(session) ?: false
        val isExtensionReceiverBorrowed = symbol.receiverParameterSymbol?.isBorrowed(session) ?: false

        val returnType = embedType(symbol.resolvedReturnType)

        val returnTarget = when {
            symbol.isPure(session) -> ReturnTarget.createForPureFunction(returnType)
            else -> returnTargetProducer.getFresh(returnType)
        }

        val signature = object : FunctionSignature {
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
                    it.embedName(), embedType(it.resolvedReturnType), it, it.isUnique(session), it.isBorrowed(session)
                )
            }
            override val returns: VariableEmbedding = returnTarget.variable
            override val isPure: Boolean = symbol.isPure(session)
        }
        return Pair(returnTarget, signature)
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


        val (preconditionContext, postconditionContext) = createContractConversionContext(signature, firSpec, returnTarget)

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
        symbol: FirFunctionSymbol<*>, signature: NamedFunctionSignature, returnTarget : ReturnTarget
    ): Pair<List<ExpEmbedding>, List<ExpEmbedding>> {
        val kotlinContractPostcondition = embedKotlinContract(signature)
        val userContract = embedFormverContract(symbol, signature, returnTarget)
        return Pair(userContract.first, kotlinContractPostcondition + userContract.second)
    }

    private fun embedFullSignature(symbol: FirFunctionSymbol<*>): Pair<ReturnTarget, FullNamedFunctionSignature> {
        val (returnTarget, signature) = embedFunctionSignature(symbol)
        val subSignature = object : NamedFunctionSignature, FunctionSignature by signature {
            override val name = symbol.embedName(this@ProgramConverter)
            override val labelName: String
                get() = super<NamedFunctionSignature>.labelName
            override val symbol = symbol
        }
        val fullSignature = if (symbol is FirConstructorSymbol && symbol.isPrimary) {
            // Constructor
            val constructedClassSymbol =
                symbol.resolvedReturnType.toRegularClassSymbol(session) ?: throw SnaktInternalException(
                    symbol.source, "Constructor does not return a regular class"
                )
            val parameterMatching = constructedClassSymbol.propertySymbols.mapNotNull { propertySymbol ->
                val name = propertySymbol.embedMemberPropertyName()
                propertySymbol.withConstructorParam { paramSymbol ->
                    typeResolver.lookupBackingField(name)?.let { paramSymbol to it }
                }
            }.toMap()

            val fieldPostconditions = signature.params.mapNotNull { param ->
                require(param is FirVariableEmbedding) { "Constructor parameters must be represented by FirVariableEmbeddings" }
                parameterMatching[param.symbol]?.let { field ->
                    (field.accessPolicy == AccessPolicy.ALWAYS_READABLE).ifTrue {
                        EqCmp(FieldAccess(signature.returns, field), param)
                    }
                }
            }
            ConstructorSignature(subSignature, symbol, fieldPostconditions, typeResolver)
        } else {
            val (preconditions, postconditions) = embedProvidedContract(symbol, subSignature, returnTarget)

            UserFunctionSignature(
                subSignature,
                symbol,
                preconditions,
                postconditions,
                typeResolver,
            )
        }

        return Pair(returnTarget, fullSignature)

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
        symbol: FirPropertySymbol
    ): FieldEmbedding? {
        val classSymbol = symbol.dispatchReceiverType?.toClassSymbol(session) as? FirRegularClassSymbol ?: return null
        val embedding = embedClass(classSymbol)
        val scopedName = symbol.callableId!!.embedMemberBackingFieldName(
            Visibilities.isPrivate(symbol.visibility)
        )
        val fieldIsAllowed = symbol.hasBackingField && !symbol.isCustom && (symbol.isFinal || classSymbol.isFinal)
        val backingField = fieldIsAllowed.ifTrue {
            UserFieldEmbedding(
                scopedName,
                embedType(symbol.resolvedReturnType),
                symbol,
                symbol.isUnique(session),
                embedding,
                symbol.isManual(session)
            )
        }
        return backingField
    }

    private fun embedCustomProperty(symbol: FirPropertySymbol) = PropertyEmbedding(
        CustomGetter(embedGetterFunction(symbol)),
        symbol.isVar.ifTrue { CustomSetter(embedSetterFunction(symbol)) },
    )

    @OptIn(SymbolInternals::class)
    private fun embedCallable(
        symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature
    ): RichCallableEmbedding {
        return if (symbol.shouldBeInlined) {
            InlineNamedFunction(signature, symbol.fir.body!!)
        } else {
            NonInlineNamedFunction(signature)
        }
    }

    private fun embedCallable(symbol: FirFunctionSymbol<*>) : RichCallableEmbedding {
        val (_, fullSignature) = embedFullSignature(symbol)
        return embedCallable(symbol, fullSignature)
    }

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
                if (classLikeSymbol.isAdt(session)) {
                    existing(embedAdtClass(classLikeSymbol))
                } else {
                    existing(embedClass(classLikeSymbol))
                }
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
            errorCollector.addMinorError("Requested type $type, for which we do not yet have an embedding.")
            unit()
        }
    }
}
