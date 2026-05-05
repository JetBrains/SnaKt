/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.lookupTagIfAny
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.properties.FinalFieldGetter
import org.jetbrains.kotlin.formver.core.embeddings.types.*
import org.jetbrains.kotlin.formver.core.linearization.pureToViper
import org.jetbrains.kotlin.formver.core.names.DispatchReceiverName
import org.jetbrains.kotlin.formver.core.names.FunctionResultVariableName
import org.jetbrains.kotlin.formver.core.names.PlaceholderReturnVariableName
import org.jetbrains.kotlin.formver.core.names.embedMemberPropertyName
import org.jetbrains.kotlin.formver.core.purity.preorder
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.*
import kotlin.collections.plus

interface FullNamedFunctionSignature : NamedFunctionSignature {
    /**
     * Preconditions of function in form of `ExpEmbedding`s with type `boolType()`.
     */
    fun getPreconditions(typeResolver: TypeResolver): List<ExpEmbedding>

    /**
     * Postconditions of function in form of `ExpEmbedding`s with type `boolType()`.
     */
    fun getPostconditions(returnVariable: VariableEmbedding, typeResolver: TypeResolver): List<ExpEmbedding>

    val declarationSource: KtSourceElement?
}

/**
 * We generate very reduced methods for getters and setters.
 * They don't have bodies or any invariants.
 * Since after the call to getter, invariants for the result will be inhaled based on the difference
 * between the returned `TypeEmbedding` and expected, we return the broadest possible type here.
 * Types of the arguments don't matter at all, but intuitively they must be `NullableAnyTypeEmbedding` as well.
 */
abstract class PropertyAccessorFunctionSignature(
    override val name: SymbolicName,
    propertySymbol: FirPropertySymbol,
) : FullNamedFunctionSignature, GenericFunctionSignatureMixin() {
    override fun getPreconditions(typeResolver: TypeResolver) = emptyList<ExpEmbedding>()
    override fun getPostconditions(returnVariable: VariableEmbedding, typeResolver: TypeResolver) = emptyList<ExpEmbedding>()
    override val dispatchReceiver: VariableEmbedding
        get() = PlaceholderVariableEmbedding(DispatchReceiverName, buildType { nullableAny() })
    override val extensionReceiver = null
    override val declarationSource: KtSourceElement? = propertySymbol.source
}

class GetterFunctionSignature(name: SymbolicName, symbol: FirPropertySymbol) :
    PropertyAccessorFunctionSignature(name, symbol) {
    override val symbol: FirFunctionSymbol<*>
        get() = error {
            "Getter symbol should not be accessed directly as it is allowed to be null in some cases."
        }
    override val callableType: FunctionTypeEmbedding = buildFunctionPretype {
        withDispatchReceiver { nullableAny() }
        withReturnType {
            // TODO: How could this be made better?
            when (symbol.resolvedReturnType.lookupTagIfAny?.name?.asString()) {
                "String" -> string()
                else -> nullableAny()
            }
        }
    }
}


class OpenGetterFunctionSignature(
    name: SymbolicName, val propertySymbol: FirPropertySymbol
) : PropertyAccessorFunctionSignature(name, propertySymbol) {

    override val symbol: FirFunctionSymbol<*>
        get() = error {
            "Getter symbol should not be accessed directly as it is allowed to be null in some cases."
        }

    override val callableType: FunctionTypeEmbedding get() = buildFunctionPretype {
        withDispatchReceiver { nullableAny() }
        withReturnType {
            nullableAny()
        }
    }
    override fun getPostconditions(returnVariable: VariableEmbedding, typeResolver: TypeResolver): List<ExpEmbedding> {
        val finalPropertyName =
            propertySymbol.callableId!!.embedMemberPropertyName(Visibilities.isPrivate(propertySymbol.visibility), true)
        val openPropertyName =
            propertySymbol.callableId!!.embedMemberPropertyName(Visibilities.isPrivate(propertySymbol.visibility), false)
        val openProperties = typeResolver.propertiesByPropertyName(openPropertyName)
        val closedProperties = typeResolver.propertiesByPropertyName(finalPropertyName)
        return openProperties.map { (name, property) ->
            val classTypeEmbedding = typeResolver.lookupClassTypeEmbedding(name.className)!!
            OperatorExpEmbeddings.Implies(
                Is(dispatchReceiver, classTypeEmbedding.asTypeEmbedding()),
                Is(returnVariable, property.type)
            )
        } + closedProperties.mapNotNull { (name, property) ->
            val classTypeEmbedding = typeResolver.lookupClassTypeEmbedding(name.className)!!
            val function = (property.getter!! as? FinalFieldGetter)?.getter as? PureUserFunctionEmbedding
            function?.let {
                OperatorExpEmbeddings.Implies(
                    Is(dispatchReceiver, classTypeEmbedding.asTypeEmbedding()),
                    EqCmp(returnVariable, FunctionCall(it.callable, listOf(dispatchReceiver)))
                )
            }
        }
    }

}



class SetterFunctionSignature(name: SymbolicName, symbol: FirPropertySymbol) :
    PropertyAccessorFunctionSignature(name, symbol) {
    override val symbol: FirFunctionSymbol<*>
        get() = error {
            "Setter symbol should not be accessed directly as it is allowed to be null in some cases."
        }
    override val callableType: FunctionTypeEmbedding = buildFunctionPretype {
        withDispatchReceiver { nullableAny() }
        withParam { nullableAny() }
        withReturnType { unit() }
    }
}

fun FullNamedFunctionSignature.toViperMethod(
    body: Stmt.Seqn?,
    returnVariable: VariableEmbedding,
    ctx: TypeResolver,
) = UserMethod(
    name,
    formalArgs.map { it.toLocalVarDecl() },
    returnVariable.toLocalVarDecl(),
    getPreconditions(ctx).pureToViper(toBuiltin = true, ctx),
    getPostconditions(returnVariable, ctx).pureToViper(toBuiltin = true, ctx),
    body,
    declarationSource.asPosition
)

fun FullNamedFunctionSignature.toViperFunction(
    ctx: TypeResolver,
    body: Exp?,
): UserFunction {
    val postconditions = getPostconditions(
        PlaceholderVariableEmbedding(
            FunctionResultVariableName, this.callableType.returnType,
        ),
        ctx
    )
    postconditions.forEach { postcondition ->
        val isValid =
            postcondition.preorder().all { it.first !is AccEmbedding && it.first !is PredicateAccessPermissions }
        if (!isValid) throw SnaktInternalException(
            declarationSource, "Postcondition tries to acquire permissions, which is not allowed in a function"
        )
    }
    val preconditions = getPreconditions(ctx)
    return UserFunction(
        name,
        formalArgs.map { it.toLocalVarDecl() },
        // TODO: Be explicit about the return types of functions instead of boxing them into a Ref
        Type.Ref,
        preconditions.pureToViper(toBuiltin = true, ctx),
        postconditions.pureToViper(toBuiltin = true, ctx),
        body,
        declarationSource.asPosition
    )
}
