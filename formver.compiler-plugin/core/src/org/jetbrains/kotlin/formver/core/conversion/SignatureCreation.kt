package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.isPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.embeddings.callables.*
import org.jetbrains.kotlin.formver.core.embeddings.expression.EqCmp
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.FirVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.PlaceholderVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.isBorrowed
import org.jetbrains.kotlin.formver.core.isPure
import org.jetbrains.kotlin.formver.core.isUnique
import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.viper.SymbolicName

data class SignatureWithTarget<out S : FunctionSignature>(
    val signature: S, val returnTarget: ReturnTarget
) {
    /**
     * Refines the signature by applying the given action to the current signature.
     * The return target is preserved.
     *
     * It should be used to make a signature more specific, e.g., to add pre+post conditions to an existing signature
     */
    fun <T : FunctionSignature> refineSignature(action: (SignatureWithTarget<S>) -> T): SignatureWithTarget<T> =
        SignatureWithTarget(action(this), returnTarget)
}

@OptIn(DirectDeclarationsAccess::class)
val FirRegularClassSymbol.propertySymbols: List<FirPropertySymbol>
    get() = declarationSymbols.filterIsInstance<FirPropertySymbol>()

private fun <R> FirPropertySymbol.withConstructorParam(action: FirPropertySymbol.(FirValueParameterSymbol) -> R): R? =
    correspondingValueParameterFromPrimaryConstructor?.let { param ->
        action(param)
    }

private val FirFunctionSymbol<*>.containingPropertyOrSelf
    get() = when (this) {
        is FirPropertyAccessorSymbol -> propertySymbol
        else -> this
    }

val FirFunctionSymbol<*>.receiverType: ConeKotlinType?
    get() = containingPropertyOrSelf.dispatchReceiverType

val FirFunctionSymbol<*>.extensionReceiverType: ConeKotlinType?
    get() = containingPropertyOrSelf.resolvedReceiverTypeRef?.coneType

context(converter: ProgramConversionContext)
fun FirFunctionSymbol<*>.toFunctionSignature(): SignatureWithTarget<FunctionSignature> {
    val dispatchReceiverType = this.receiverType
    val extensionReceiverType = this.extensionReceiverType

    val returnType = converter.embedType(this.resolvedReturnType)

    val returnTarget = when {
        this.isPure(converter.session) -> ReturnTarget.createForPureFunction(returnType)
        else -> converter.returnTargetProducer.getFresh(returnType)
    }

    val dispatchVariable = dispatchReceiverType?.let {
        PlaceholderVariableEmbedding(
            DispatchReceiverName,
            converter.embedType(it),
            isUnique = false,
            isBorrowed = false,
        )
    }
    val extensionVariable = extensionReceiverType?.let {
        PlaceholderVariableEmbedding(
            ExtensionReceiverName,
            converter.embedType(it),
            this.receiverParameterSymbol?.isUnique(converter.session) ?: false,
            this.receiverParameterSymbol?.isBorrowed(converter.session) ?: false,
        )
    }

    val parameterVariables = this.valueParameterSymbols.map {
        FirVariableEmbedding(
            it.embedName(),
            converter.embedType(it.resolvedReturnType),
            it,
            it.isUnique(converter.session),
            it.isBorrowed(converter.session)
        )
    }

    val signature = FunctionSignatureImpl(
        converter.embedFunctionPretype(this),
        dispatchVariable,
        extensionVariable,
        parameterVariables,
        returns = returnTarget.variable,
        isPure = this.isPure(converter.session)
    )

    return SignatureWithTarget(signature, returnTarget)
}

context(converter: ProgramConversionContext)
fun FunctionTypeEmbedding.toGenericAccessorSignature(isPure: Boolean): SignatureWithTarget<FunctionSignature> {
    val returnTarget = when (isPure) {
        true -> ReturnTarget.createForPureFunction(returnType)
        false -> converter.returnTargetProducer.getFresh(returnType)
    }
    val extensionVariable = extensionReceiverType?.let { PlaceholderVariableEmbedding(ExtensionReceiverName, it) }
    val dispatchVariable = dispatchReceiverType?.let { PlaceholderVariableEmbedding(DispatchReceiverName, it) }
    val parameterVariables =
        paramTypes.mapIndexed { index, embedding -> PlaceholderVariableEmbedding(AnonymousName(index), embedding) }
    val signature = FunctionSignatureImpl(
        this,
        dispatchVariable,
        extensionVariable,
        parameterVariables,
        returnTarget.variable,
        isPure
    )
    return SignatureWithTarget(signature, returnTarget)
}


context(converter: ProgramConversionContext)
fun SignatureWithTarget<FunctionSignature>.toNamedSignature(symbol: FirFunctionSymbol<*>): SignatureWithTarget<NamedFunctionSignature> =
    this.refineSignature { current ->
        NamedFunctionSignatureImpl(current.signature, symbol.embedName(converter), symbol)
    }

fun SignatureWithTarget<FunctionSignature>.toNamedSignature(name: SymbolicName): SignatureWithTarget<NamedFunctionSignature> =
    this.refineSignature { current ->
        NamedFunctionSignatureImpl(current.signature, name, null)
    }


fun SignatureWithTarget<NamedFunctionSignature>.toNonInlineSignature(symbol: FirFunctionSymbol<*>?): SignatureWithTarget<NonInlineCallable> =
    this.refineSignature { current ->
        NonInlineCallableImpl(current.signature, symbol)
    }


context(converter: ProgramConversionContext)
fun SignatureWithTarget<NonInlineCallable>.toCompleteSignature(symbol: FirFunctionSymbol<*>): SignatureWithTarget<NonInlineFunctionSignature> =
    when {
        symbol.isPrimaryConstructor() -> this.toConstructorSignature(symbol)
        else -> this.toNormalSignature(symbol)
    }

context(converter: ProgramConversionContext)
fun SignatureWithTarget<NonInlineCallable>.toCompleteSignature(
    declarationSource: KtSourceElement?, action: FunctionConditionBuilder.() -> Unit
): SignatureWithTarget<NonInlineFunctionSignature> = refineSignature { current ->
    val (preconditions, postconditions) = current.signature.buildConditions(converter.typeResolver, action)
    NonInlineFunctionSignature(current.signature, preconditions, postconditions, declarationSource)
}


context(converter: ProgramConversionContext)
fun SignatureWithTarget<NonInlineCallable>.toConstructorSignature(symbol: FirFunctionSymbol<*>): SignatureWithTarget<NonInlineFunctionSignature> =
    refineSignature { current ->
        val constructedClassSymbol =
            symbol.resolvedReturnType.toRegularClassSymbol(converter.session) ?: throw SnaktInternalException(
                symbol.source, "Constructor does not return a regular class"
            )
        val parameterMatching = constructedClassSymbol.propertySymbols.mapNotNull { propertySymbol ->
            val name = propertySymbol.embedMemberPropertyName(converter)
            propertySymbol.withConstructorParam { paramSymbol ->
                converter.typeResolver.lookupDefaultBehavingProperties(name)?.let { paramSymbol to it }
            }
        }.toMap()

        val fieldPostconditions = current.signature.params.mapNotNull { param ->
            require(param is FirVariableEmbedding) { "Constructor parameters must be represented by FirVariableEmbeddings" }
            parameterMatching[param.symbol]?.let { property ->
                EqCmp(property.getter!!.getValueSimple(returnTarget.variable, converter.typeResolver), param)
            }
        }

        val contract = current.signature.buildConditions(converter.typeResolver) {
            userFunctionContract()
            addPostconditions(fieldPostconditions)
        }

        NonInlineFunctionSignature(current.signature, contract.preconditions, contract.postconditions, symbol.source)
    }

context(converter: ProgramConversionContext)
fun SignatureWithTarget<NonInlineCallable>.toNormalSignature(symbol: FirFunctionSymbol<*>): SignatureWithTarget<NonInlineFunctionSignature> =
    refineSignature { current ->
        val contract = current.signature.buildConditions(converter.typeResolver) {
            userFunctionContract()
            val (preconditions, postconditions) = converter.embedContract(
                symbol, current.signature, returnTarget
            )
            addPreconditions(preconditions)
            addPostconditions(postconditions)
            addPostconditions(dataClassGeneratedPostconditions(symbol, current.signature, returnTarget))
        }
        NonInlineFunctionSignature(current.signature, contract.preconditions, contract.postconditions, symbol.source)
    }

context(converter: ProgramConversionContext)
private fun dataClassGeneratedPostconditions(
    symbol: FirFunctionSymbol<*>,
    signature: NamedFunctionSignature,
    returnTarget: ReturnTarget,
): List<ExpEmbedding> {
    val dataClass = symbol.receiverType?.toRegularClassSymbol(converter.session)?.takeIf { it.isData }
        ?: return emptyList()
    val receiver = signature.dispatchReceiver ?: return emptyList()
    val constructorProperties = dataClass.propertySymbols.mapNotNull { property ->
        property.correspondingValueParameterFromPrimaryConstructor?.let { parameter ->
            val embedding = converter.embedProperty(property)
            if (embedding.getter == null) null else Triple(property, parameter, embedding)
        }
    }

    val componentIndex = symbol.name.asString().removePrefix("component").toIntOrNull()
    if (componentIndex != null && symbol.valueParameterSymbols.isEmpty()) {
        val property = constructorProperties.getOrNull(componentIndex - 1)?.third ?: return emptyList()
        return listOf(EqCmp(returnTarget.variable, property.getter!!.getValueSimple(receiver, converter.typeResolver)))
    }

    if (symbol.name.asString() != "copy") return emptyList()
    val paramsByName = signature.params.filterIsInstance<FirVariableEmbedding>().mapNotNull { parameter ->
        (parameter.symbol as? FirValueParameterSymbol)?.name?.let { it to parameter }
    }.toMap()
    val propertyEqualities = constructorProperties.mapNotNull { (_, constructorParameter, property) ->
        val parameter = paramsByName[constructorParameter.name] ?: return@mapNotNull null
        EqCmp(property.getter!!.getValueSimple(returnTarget.variable, converter.typeResolver), parameter)
    }
    // `copy` returns a freshly created instance, so - like the constructor - it must grant the
    // caller exclusive access to the result's unique predicate. Without it, the copy result could
    // not be mutated or passed where uniqueness is required, even though the constructor allows it.
    val uniqueAccess = returnTarget.variable.uniquePredicateAccessInvariant(converter.typeResolver)
    return propertyEqualities + listOfNotNull(uniqueAccess)
}

@OptIn(SymbolInternals::class)
context(converter: ProgramConversionContext)
fun SignatureWithTarget<NamedFunctionSignature>.toInlineSignature(symbol: FirFunctionSymbol<*>): SignatureWithTarget<InlineNamedFunction> =
    this.refineSignature { current ->
        val body = symbol.fir.body ?: throw SnaktInternalException(symbol.source, "Expected function body, got null")
        val contract = current.signature.buildConditions(converter.typeResolver) {
            val (precondition, postcondition) = converter.embedContract(
                symbol, current.signature, current.returnTarget
            )
            userFunctionContract()
            addPreconditions(precondition)
            addPostconditions(postcondition)
        }

        val fullSignature = InlineNamedFunction(
            current.signature, body, contract.preconditions, contract.postconditions, symbol
        )
        fullSignature
    }
