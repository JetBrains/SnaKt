/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.formver.common.ErrorCollector
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.common.UnsupportedFeatureBehaviour
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
    private val specialFunctions: Map<SymbolicName, SpecialKotlinFunction> = buildMap {
        putAll(SpecialKotlinFunctions.byName)
        putAll(PartiallySpecialKotlinFunctions.generateAllByName())
    }
    private val impureFunctions: MutableMap<SymbolicName, UserFunctionEmbedding> = mutableMapOf()
    private val pureFunctions: MutableMap<SymbolicName, PureUserFunctionEmbedding> = mutableMapOf()

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
     */
    fun convertAll() {
        for ((declaration, signature, returnTarget) in registered) {
            val stmtCtx = createBodyConversionContext(signature, returnTarget)
            if (signature.isPure) {
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
            val source = impureFunctions[name]?.callable?.declarationSource
            body.bodyExp.checkValidity(source, errorCollector)
        }
        convertedBodyResolver.forEachPure { name, body ->
            if (!body.isPure()) {
                val source = pureFunctions[name]?.callable?.declarationSource
                errorCollector.addPurityError(source, "Impure function body detected in pure function")
            }
        }
    }

    /**
     * Build the finalized Viper `Method` / `Function` for every embedded callable, storing each in
     * [linearizedBodyResolver]. Should only be invoked when [errorCollector] is clean.
     *
     * Iterates the embedding maps directly (rather than the converted-body map) so that callables
     * without a body still get a Viper header.
     */
    fun linearizeAll() {
        for ((name, special) in specialFunctions) {
            // Fully-special functions never produce a Viper method; partially-special ones produce a
            // header only when their base embedding has been initialised.
            val callable = (special as? PartiallySpecialKotlinFunction)?.baseEmbedding?.callable ?: continue
            callable.toViperMethodHeader(typeResolver)?.let { linearizedBodyResolver.storeMethod(name, it) }
        }
        for ((name, embedding) in impureFunctions) {
            val callable = embedding.callable
            val source = callable.declarationSource
            val converted = convertedBodyResolver.lookupImpure(name)
            val method = if (converted != null) {
                linearizeImpureBody(source, converted).toViperMethod(callable, typeResolver)
            } else {
                callable.toViperMethodHeader(typeResolver)
            }
            if (method != null) linearizedBodyResolver.storeMethod(name, method)
        }
        for ((name, embedding) in pureFunctions) {
            val converted = convertedBodyResolver.lookupPure(name)
            val linearized = converted?.let { linearizePureBody(embedding.callable.declarationSource, it) }
            linearizedBodyResolver.storeFunction(name, embedding.viperFunction(typeResolver, linearized))
        }
    }

    private fun ensurePureUserFunctionEmbedding(
        symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature
    ): PureUserFunctionEmbedding = pureFunctions.getOrPut(signature.name) {
        PureUserFunctionEmbedding(embedCallable(symbol, signature))
    }

    @OptIn(SymbolInternals::class)
    private fun embedPureUserFunction(
        symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature, returnTarget: ReturnTarget
    ): PureUserFunctionEmbedding {
        pureFunctions[signature.name]?.also { return it }
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
        impureFunctions[name]?.let { return it }
        return UserFunctionEmbedding(embedCallable(symbol, signature)).also {
            impureFunctions[name] = it
        }
    }

    private fun embedOpenGetterFunction(symbol: FirPropertySymbol): CallableEmbedding {
        val name = symbol.embedGetterName(this)
        return impureFunctions.getOrPut(name) {
            val signature = OpenGetterFunctionSignature(name, symbol)
            UserFunctionEmbedding(
                NonInlineNamedFunction(signature)
            )
        }
    }

    private fun embedClosedGetterFunction(symbol: FirPropertySymbol): CallableEmbedding {
        val name = symbol.embedGetterName(this)
        return pureFunctions.getOrPut(name) {
            val classType = embedType(symbol.dispatchReceiverType!!)
            val returnType = embedType(symbol.resolvedReturnType)
            val signature = ClosedGetterFunctionSignature(name, symbol, classType, returnType)
            PureUserFunctionEmbedding(
                NonInlineNamedFunction(signature)
            )
        }
    }

    private fun embedSetterFunction(symbol: FirPropertySymbol): CallableEmbedding {
        val name = symbol.embedSetterName(this)
        return impureFunctions.getOrPut(name) {
            val signature = SetterFunctionSignature(name, symbol)
            UserFunctionEmbedding(
                NonInlineNamedFunction(signature)
            )
        }
    }

    override fun embedFunction(symbol: FirFunctionSymbol<*>): CallableEmbedding {
        val lookupName = symbol.embedName(this)
        specialFunctions[lookupName]?.let { existing ->
            if (existing !is PartiallySpecialKotlinFunction) return existing
            if (existing.baseEmbedding != null) return existing
            val callable = embedCallable(symbol)
            existing.initBaseEmbedding(UserFunctionEmbedding(callable))
            return existing
        }
        impureFunctions[lookupName]?.let { return it }
        val callable = embedCallable(symbol)
        return UserFunctionEmbedding(callable).also {
            impureFunctions[lookupName] = it
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

    @OptIn(SymbolInternals::class)
    override fun embedProperty(symbol: FirPropertySymbol): PropertyEmbedding {

        if (symbol.receiverParameterSymbol != null) {
            return embedCustomProperty(symbol)
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
                    symbol.source,
                    "Properties dispatch receiver is not a regular class"
                )

                val wellBehaved = isGuaranteedDefaultProperty(symbol)
                val isImmutable = symbol.isVal
                val isManual = symbol.isManual(session)

                return@getOrPutProperty when {
                    (wellBehaved && !isImmutable) || isManual -> {
                        // we can use a backing field to represent it
                        val field = embedBackingField(symbol, regularClass)
                        PropertyEmbedding(
                            BackingFieldGetter(field), BackingFieldSetter(field), hasDefaultBehaviour = true
                        )
                    }

                    wellBehaved -> {
                        // we can replace it with a function call
                        PropertyEmbedding(
                            CustomGetter(embedClosedGetterFunction(symbol)),
                            null,
                            hasDefaultBehaviour = true
                        )
                    }

                    else -> {
                        // The property could be overwritten by anything, or has a custom getter/setter.
                        // We can only reason about the upper bound of the type.
                        PropertyEmbedding(
                            CustomGetter(embedOpenGetterFunction(symbol)),
                            symbol.isVar.ifTrue { CustomSetter(embedSetterFunction(symbol)) },
                            hasDefaultBehaviour = false
                        )
                    }
                }
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
                val name = propertySymbol.embedMemberPropertyName(this)
                propertySymbol.withConstructorParam { paramSymbol ->
                    typeResolver.lookupDefaultBehavingProperties(name)?.let { paramSymbol to it }
                }
            }.toMap()

            val context = createBodyConversionContext(subSignature, returnTarget)

            val fieldPostconditions = signature.params.mapNotNull { param ->
                require(param is FirVariableEmbedding) { "Constructor parameters must be represented by FirVariableEmbeddings" }
                parameterMatching[param.symbol]?.let { property ->
                    EqCmp(property.getter!!.getValueSimple(returnTarget.variable, context), param)
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
        symbol: FirPropertySymbol,
        classSymbol : FirRegularClassSymbol
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

    private fun embedCustomProperty(symbol: FirPropertySymbol) = PropertyEmbedding(
        CustomGetter(embedOpenGetterFunction(symbol)),
        symbol.isVar.ifTrue { CustomSetter(embedSetterFunction(symbol)) },
        hasDefaultBehaviour = false
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

    override fun isGuaranteedDefaultProperty(symbol: FirPropertySymbol): Boolean {
        val classSymbolFinal = symbol.dispatchReceiverType?.toClassSymbol(session)?.isFinal ?: false
        return (symbol.isFinal || classSymbolFinal) && !symbol.isCustom
    }
}
