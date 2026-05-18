/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
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
import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.AdtDecl
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
    private val methods: MutableMap<SymbolicName, FunctionEmbedding> = buildMap {
        putAll(SpecialKotlinFunctions.byName)
        putAll(PartiallySpecialKotlinFunctions.generateAllByName())
    }.toMutableMap()
    private val functions: MutableMap<SymbolicName, PureFunctionEmbedding> = mutableMapOf()

    override val typeResolver: TypeResolver = TypeResolver()

    // Cast is valid since we check that values are not null. We specify the type for `filterValues` explicitly to ensure there's no
    // loss of type information earlier.
    @Suppress("UNCHECKED_CAST")
    val debugExpEmbeddings: Map<SymbolicName, ExpEmbedding>
        get() = methods.mapValues { (it.value as? UserFunctionEmbedding)?.body?.debugExpEmbedding() }
            .filterValues { value: ExpEmbedding? -> value != null } as Map<SymbolicName, ExpEmbedding>


    override val whileIndexProducer = indexProducer()
    override val catchLabelNameProducer = simpleFreshEntityProducer(::CatchLabelName)
    override val tryExitLabelNameProducer = simpleFreshEntityProducer(::TryExitLabelName)
    override val scopeIndexProducer = scopeIndexProducer()

    // The type annotation is necessary for the code to compile.
    override val anonVarProducer = FreshEntityProducer(::AnonymousVariableEmbedding)
    override val anonBuiltinVarProducer = FreshEntityProducer(::AnonymousBuiltinVariableEmbedding)
    override val returnTargetProducer = FreshEntityProducer(ReturnTarget::createForDepth)
    override val nameResolver = ShortNameResolver()

    val program: Program
        get() = Program(
            domains = listOf(RuntimeTypeDomain(typeResolver)),
            // We need to deduplicate fields since public fields with the same name are represented differently
            // at `FieldEmbedding` level but map to the same Viper.
            fields = typeResolver.backingFields().distinctBy { it.name }.map { it.toViper() },
            functions = SpecialFunctions.all + functions.values.mapNotNull { it.viperFunction(typeResolver) }
                .distinctBy { it.name },
            methods = SpecialMethods.all + methods.values.mapNotNull { it.viperMethod(typeResolver) }
                .distinctBy { it.name } + typeResolver.backingFields().map { it.type.havocMethod(typeResolver) }
                .distinctBy { it.name },
            predicates = typeResolver.classTypeEmbeddings().flatMap {
                with(typeResolver) {
                    listOf(
                        it.sharedPredicate(), it.uniquePredicate()
                    )
                }
            },
            adts = typeResolver.adtTypeEmbeddings().map { constructAdtDecl(it) },
        )


    fun registerForVerification(declaration: FirSimpleFunction) {
        // Note: it is important that `body` is only set after embedding is complete, as we need to
        // place the embedding in the map before processing the body.
        if (declaration.symbol.isPure(session)) {
            embedPureFunction(declaration.symbol)
        } else {
            embedUserFunction(declaration)
        }
        if (errorCollector.collectedAdtError()) {
            errorCollector.addAdtError(
                declaration.source,
                "Function '${declaration.name.asString()}' references an invalid ADT",
            )
        }
    }

    @OptIn(SymbolInternals::class)
    private fun embedPureUserFunction(
        symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature, returnTarget: ReturnTarget
    ): PureUserFunctionEmbedding {
        (functions[signature.name] as? PureUserFunctionEmbedding)?.also { return it }
        val new = PureUserFunctionEmbedding(embedCallable(symbol, signature))
        // Insert into the map before processing the body, so recursive calls can find the embedding.
        functions[signature.name] = new
        val declaration = symbol.fir as? FirSimpleFunction ?: throw SnaktInternalException(
            symbol.source, "Expected FirSimpleFunction, got unexpected type ${symbol.fir.javaClass.simpleName}"
        )
        if (declaration.body != null) {
            val stmtCtx = createBodyConversionContext(signature, returnTarget)
            new.body = stmtCtx.convertFunctionWithBody(declaration)
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

    fun embedUserFunction(declaration: FirSimpleFunction): UserFunctionEmbedding {
        val symbol = declaration.symbol
        val name = symbol.embedName(this)
        (methods[name] as? UserFunctionEmbedding)?.also { return it }
        val (returnTarget, fullSignature) = embedFullSignature(symbol)
        val new = UserFunctionEmbedding(embedCallable(symbol, fullSignature)).apply {
            val stmtCtx = createBodyConversionContext(fullSignature, returnTarget)
            body = stmtCtx.convertMethodWithBody(declaration, fullSignature)
        }
        methods[name] = new
        return new
    }

    private fun embedGetterFunction(symbol: FirPropertySymbol): FunctionEmbedding {
        val name = symbol.embedGetterName(this)
        return methods.getOrPut(name) {
            val signature = GetterFunctionSignature(name, symbol)
            UserFunctionEmbedding(
                NonInlineNamedFunction(signature)
            )
        }
    }

    private fun embedSetterFunction(symbol: FirPropertySymbol): FunctionEmbedding {
        val name = symbol.embedSetterName(this)
        return methods.getOrPut(name) {
            val signature = SetterFunctionSignature(name, symbol)
            UserFunctionEmbedding(
                NonInlineNamedFunction(signature)
            )
        }
    }

    override fun embedFunction(symbol: FirFunctionSymbol<*>): FunctionEmbedding {
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

    override fun embedPureFunction(symbol: FirFunctionSymbol<*>): PureFunctionEmbedding {
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

    @OptIn(DirectDeclarationsAccess::class)
    private fun embedAdtClass(symbol: FirRegularClassSymbol): AdtTypeEmbedding {
        val name = symbol.classId.embedName()
        typeResolver.lookupAdtTypeEmbedding(name)?.let { return it }
        if (!adtHeaderIsValid(symbol)) {
            typeResolver.registerAdt(name, InvalidAdtTypeEmbedding)
            return InvalidAdtTypeEmbedding
        }
        val adtEmbedding = AdtTypeEmbeddingImpl(name)
        typeResolver.registerAdt(name, adtEmbedding)
        var allValid = true
        symbol.declarationSymbols.forEach { sym ->
            if (!embedAdtProperty(sym, adtEmbedding)) allValid = false
        }
        if (!allValid) typeResolver.registerAdt(name, InvalidAdtTypeEmbedding)
        else {
            val primaryCtor = symbol.declarationSymbols
                .filterIsInstance<FirConstructorSymbol>()
                .first { it.isPrimary }
            methods[primaryCtor.embedName(this)] =
                AdtConstructorEmbedding(adtEmbedding, typeResolver.lookupAdtFields(adtEmbedding.name))
        }
        return if (allValid) adtEmbedding else InvalidAdtTypeEmbedding
    }

    private fun adtHeaderIsValid(symbol: FirRegularClassSymbol): Boolean {
        if (!symbol.isData) {
            errorCollector.addAdtError(
                symbol.source,
                "Invalid ADT annotation: @ADT may only be applied to data class or data object declarations",
            )
            return false
        }
        if (symbol.typeParameterSymbols.isNotEmpty()) {
            errorCollector.addAdtError(
                symbol.source,
                "Invalid ADT annotation: An @ADT declaration must not have type parameters",
            )
            return false
        }
        if (symbol.resolvedSuperTypes.any { !it.isAny }) {
            errorCollector.addAdtError(
                symbol.source,
                "Invalid ADT annotation: An @ADT declaration must not extend a class or implement an interface",
            )
            return false
        }
        return true
    }

    private fun embedAdtProperty(symbol: FirBasedSymbol<*>, adtEmbedding: AdtTypeEmbeddingImpl): Boolean {
        var valid = true
        fun rejectProperty(reason: String) {
            errorCollector.addAdtError(symbol.source, "Invalid ADT annotation: $reason")
            valid = false
        }
        when (symbol) {
            is FirPropertySymbol -> {
                if (symbol.correspondingValueParameterFromPrimaryConstructor == null) {
                    rejectProperty("An @ADT declaration may only declare fields in its primary constructor")
                }
                if (symbol.isVar) {
                    rejectProperty("An @ADT declaration may only declare immutable (val) fields")
                }
                if (!valid) return false
                val field = typeResolver.getOrPutAdtField(symbol.embedMemberPropertyName()) {
                    AdtFieldEmbedding(
                        name = AdtFieldName(adtEmbedding.adtName, SimpleKotlinName(symbol.name)),
                        type = embedType(symbol.resolvedReturnType),
                    )
                }
                typeResolver.getOrPutProperty(symbol.embedMemberPropertyName()) {
                    PropertyEmbedding(AdtFieldGetter(field, adtEmbedding), setter = null)
                }
            }
            is FirConstructorSymbol -> if (!symbol.isPrimary)
                rejectProperty("An @ADT declaration must not have secondary constructors")
            is FirNamedFunctionSymbol -> if (symbol.origin == FirDeclarationOrigin.Source)
                rejectProperty("An @ADT declaration must not have user-declared member functions")
            else -> rejectProperty("An @ADT declaration does not support symbols of type ${symbol.javaClass.simpleName}")
        }
        return valid
    }

    private fun constructAdtDecl(embedding: AdtTypeEmbeddingImpl): AdtDecl =
        AdtDecl(
            name = embedding.adtName,
            constructors = listOf(embedding.getViperConstructorDecl(typeResolver.lookupAdtFields(embedding.name))),
        )

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
