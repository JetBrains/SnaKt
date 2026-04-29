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
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.common.UnsupportedFeatureBehaviour
import org.jetbrains.kotlin.formver.core.*
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.callables.*
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.properties.*
import org.jetbrains.kotlin.formver.core.embeddings.types.*
import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Program
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

/**
 * Name used to look up properties.
 */
data class PropertyKotlinName(val scope: NameScope, val name: Name)
typealias ClassPropertyPair = Pair<ScopedName, PropertyKotlinName>

/**
 * Tracks the top-level information about the program.
 * Conversions for global entities like types should generally be
 * performed via this context to ensure they can be deduplicated.
 * We need the FirSession to get access to the TypeContext.
 */
class ProgramConverter(
    val session: FirSession, override val config: PluginConfiguration, override val errorCollector: ErrorCollector
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
    override val returnTargetProducer = FreshEntityProducer(::ReturnTarget)
    override val nameResolver = ShortNameResolver()


    val program: Program
        get() = Program(
            domains = listOf(RuntimeTypeDomain(typeResolver)),
            // We need to deduplicate fields since public fields with the same name are represented differently
            // at `FieldEmbedding` level but map to the same Viper.
            fields = SpecialFields.all.map { it.toViper() } + typeResolver.backingFields().distinctBy { it.name }
                .map { it.toViper() },
            functions = SpecialFunctions.all + functions.values.mapNotNull { it.viperFunction(typeResolver) }
                .distinctBy { it.name },
            methods = SpecialMethods.all + methods.values.mapNotNull { it.viperMethod(typeResolver) }
                .distinctBy { it.name } + typeResolver.backingFields().map { it.type.havocMethod(typeResolver) }
                .distinctBy { it.name },
            predicates = typeResolver.embeddings().flatMap {
                listOf(
                    it.sharedPredicate(typeResolver), it.uniquePredicate(typeResolver)
                )
            },
        )

    fun registerForVerification(declaration: FirSimpleFunction) {
        val signature = embedFullSignature(declaration.symbol)
        // Note: it is important that `body` is only set after embedding is complete, as we need to
        // place the embedding in the map before processing the body.
        if (declaration.symbol.isPure(session)) {
            embedPureUserFunction(declaration.symbol, signature)
        } else {
            val (returnTarget, stmtCtx) = createBodyConversionContext(signature)
            embedUserFunction(declaration.symbol, signature).apply {
                body = stmtCtx.convertMethodWithBody(declaration, signature, returnTarget)
            }
        }
    }

    @OptIn(SymbolInternals::class)
    private fun embedPureUserFunction(
        symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature
    ): PureUserFunctionEmbedding {
        (functions[signature.name] as? PureUserFunctionEmbedding)?.also { return it }
        val new = PureUserFunctionEmbedding(processCallable(symbol, signature))
        // Insert into the map before processing the body, so recursive calls can find the embedding.
        functions[signature.name] = new
        val declaration = symbol.fir as? FirSimpleFunction ?: throw SnaktInternalException(
            symbol.source, "Expected FirSimpleFunction, got unexpected type ${symbol.fir.javaClass.simpleName}"
        )
        if (declaration.body != null) {
            val (_, stmtCtx) = createBodyConversionContext(signature)
            new.body = stmtCtx.convertFunctionWithBody(declaration)
        }
        return new
    }

    private fun createBodyConversionContext(signature: FullNamedFunctionSignature): Pair<ReturnTarget, StmtConversionContext> {
        val returnTarget = returnTargetProducer.getFresh(signature.callableType.returnType)
        val paramResolver = RootParameterResolver(
            this@ProgramConverter,
            signature,
            signature.symbol.valueParameterSymbols,
            signature.labelName,
            returnTarget,
        )
        val stmtCtx = MethodConverter(
            this@ProgramConverter,
            signature,
            paramResolver,
            scopeIndexProducer.getFresh(),
        ).statementCtxt()
        return Pair(returnTarget, stmtCtx)
    }

    fun embedUserFunction(symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature): UserFunctionEmbedding {
        (methods[signature.name] as? UserFunctionEmbedding)?.also { return it }
        val new = UserFunctionEmbedding(processCallable(symbol, signature))
        methods[signature.name] = new
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
                val signature = embedFullSignature(symbol)
                val callable = processCallable(symbol, signature)
                UserFunctionEmbedding(callable).also {
                    methods[lookupName] = it
                }
            }

            is PartiallySpecialKotlinFunction -> {
                if (existing.baseEmbedding != null) return existing
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

    override fun embedPureFunction(symbol: FirFunctionSymbol<*>): PureFunctionEmbedding {
        val signature = embedFullSignature(symbol)
        return embedPureUserFunction(symbol, signature)
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
        typeResolver.lookupEmbedding(className)?.let { return it }

        val embedding = typeResolver.getOrPutEmbedding(className) {
            val embedding = buildClassPretype {
                withName(className)
            }

            typeResolver.register(embedding, symbol.classKind.isInterface)

            symbol.resolvedSuperTypes.forEach {
                val superTypeName = embedType(it).pretype.name
                typeResolver.addSubtypeRelation(className, superTypeName)
            }

            embedding
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

    override fun embedProperty(symbol: FirPropertySymbol): PropertyEmbedding {

        SpecialProperties.byCallableId[symbol.callableId]?.let {
            return it
        }

        if (symbol.receiverParameterSymbol != null) {
            return embedCustomProperty(symbol)
        } else {
            val name = symbol.embedMemberPropertyName()
            return typeResolver.getOrPutProperty(name) {
                processBackingField(symbol)?.let {
                    return@getOrPutProperty PropertyEmbedding(
                        BackingFieldGetter(it), BackingFieldSetter(it)
                    )
                }
                embedType(symbol.dispatchReceiverType!!)
                embedCustomProperty(symbol)
            }
        }
    }

    private fun <R> FirPropertySymbol.withConstructorParam(action: FirPropertySymbol.(FirValueParameterSymbol) -> R): R? =
        correspondingValueParameterFromPrimaryConstructor?.let { param ->
            action(param)
        }

    private fun extractConstructorParamsAsFields(symbol: FirFunctionSymbol<*>): Map<FirValueParameterSymbol, FieldEmbedding> {
        if (symbol !is FirConstructorSymbol || !symbol.isPrimary) return emptyMap()
        val constructedClassSymbol = symbol.resolvedReturnType.toRegularClassSymbol(session) ?: return emptyMap()
        val constructedClass = embedClass(constructedClassSymbol)
        return constructedClassSymbol.propertySymbols.mapNotNull { propertySymbol ->
            val name = propertySymbol.embedMemberPropertyName()
            propertySymbol.withConstructorParam { paramSymbol ->
                typeResolver.lookupBackingField(name)?.let { paramSymbol to it }
            }
        }.toMap()
    }

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
                    it.embedName(), embedType(it.resolvedReturnType), it, it.isUnique(session), it.isBorrowed(session)
                )
            }
        }
    }

    @OptIn(SymbolInternals::class)
    private val FirRegularClassSymbol.propertySymbols: List<FirPropertySymbol>
        get() {
            val result = mutableListOf<FirPropertySymbol>()
            this.fir.processAllDeclarations(session) {
                if (it is FirPropertySymbol) result.add(it)
            }
            return result
        }

    private fun embedFullSignature(symbol: FirFunctionSymbol<*>): FullNamedFunctionSignature {
        val subSignature = object : NamedFunctionSignature, FunctionSignature by embedFunctionSignature(symbol) {
            override val name = symbol.embedName(this@ProgramConverter)
            override val labelName: String
                get() = super<NamedFunctionSignature>.labelName
            override val symbol = symbol
        }
        val constructorParamSymbolsToFields = extractConstructorParamsAsFields(symbol)
        val contractVisitor = ContractDescriptionConversionVisitor(this@ProgramConverter, subSignature)

        @OptIn(SymbolInternals::class) val declaration = symbol.fir
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

        val rootResolver = RootParameterResolver(
            this@ProgramConverter,
            subSignature,
            subSignature.symbol.valueParameterSymbols,
            subSignature.labelName,
            ReturnTarget(depth = 0, subSignature.callableType.returnType),
        )

        fun createCtx(returnVariable: VariableEmbedding? = null): StmtConversionContext {
            val returnVarCtx = returnVariable?.let { ret -> firSpec?.returnVar?.let { ReturnVarSubstitutor(it, ret) } }
            val paramResolver = if (returnVarCtx != null) SubstitutedReturnParameterResolver(rootResolver, returnVarCtx)
            else rootResolver

            return MethodConverter(
                this@ProgramConverter,
                subSignature,
                paramResolver,
                scopeDepth = ScopeIndex.NoScope,
            ).statementCtxt()
        }

        return object : FullNamedFunctionSignature, NamedFunctionSignature by subSignature {
            override fun getPreconditions() = buildList {
                subSignature.formalArgs.forEach {
                    addAll(it.pureInvariants())
                    addAll(it.accessInvariants(typeResolver))
                    addAll(it.provenInvariants())
                    if (it.isUnique) {
                        addIfNotNull(it.type.uniquePredicateAccessInvariant(typeResolver)?.fillHole(it))
                    }
                }
                addAll(subSignature.stdLibPreconditions(typeResolver))
                // We create a separate context to embed the preconditions here.
                // Hence, some parts of FIR are translated later than the other and less explicitly.
                // TODO: this process should be a separate step in the conversion.
                firSpec?.precond?.let {
                    addAll(createCtx().collectInvariants(it))
                }
            }

            override fun getPostconditions(returnVariable: VariableEmbedding) = buildList {
                subSignature.formalArgs.forEach {
                    addAll(it.accessInvariants(typeResolver))
                    if (it.isUnique && it.isBorrowed) {
                        addIfNotNull(it.type.uniquePredicateAccessInvariant(typeResolver)?.fillHole(it))
                    }
                }
                addAll(returnVariable.pureInvariants())
                addAll(returnVariable.provenInvariants())
                if (!subSignature.symbol.isPure(session)) {
                    addAll(returnVariable.allAccessInvariants(typeResolver))
                    if (subSignature.callableType.returnsUnique) {
                        addIfNotNull(returnVariable.uniquePredicateAccessInvariant(typeResolver))
                    }
                }
                addAll(contractVisitor.getPostconditions(ContractVisitorContext(returnVariable, symbol)))
                addAll(subSignature.stdLibPostconditions(returnVariable, typeResolver))
                addIfNotNull(primaryConstructorInvariants(returnVariable))
                // TODO: this process should be a separate step in the conversion (see above)
                firSpec?.postcond?.let {
                    addAll(createCtx(returnVariable).collectInvariants(it))
                }
            }

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
    private fun processBackingField(
        symbol: FirPropertySymbol
    ): FieldEmbedding? {
        val classSymbol = symbol.dispatchReceiverType?.toClassSymbol(session) as? FirRegularClassSymbol ?: return null
        val embedding = embedClass(classSymbol)
        val scopedName = symbol.callableId!!.embedMemberBackingFieldName(
            Visibilities.isPrivate(symbol.visibility)
        )
        val fieldIsAllowed = symbol.hasBackingField && !symbol.isCustom && (symbol.isFinal || classSymbol.isFinal)
        val backingField = scopedName.specialEmbedding(embedding, typeResolver) ?: fieldIsAllowed.ifTrue {
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
    private fun processCallable(
        symbol: FirFunctionSymbol<*>, signature: FullNamedFunctionSignature
    ): RichCallableEmbedding {
        return if (symbol.shouldBeInlined) {
            InlineNamedFunction(signature, symbol.fir.body!!)
        } else {
            // We generate a dummy method header here to ensure all required types are processed already. If we skip this, any types
            // that are used only in contracts cause an error because they are not processed until too late.
            // TODO: fit this into the flow in some logical way instead.
            NonInlineNamedFunction(
                signature, symbol.isPure(session)
            ).also {
                if (symbol.isPure(session)) it.toViperFunctionHeader(typeResolver) else it.toViperMethodHeader(
                    typeResolver
                )
            }
        }
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
            errorCollector.addMinorError("Requested type $type, for which we do not yet have an embedding.")
            unit()
        }
    }
}
