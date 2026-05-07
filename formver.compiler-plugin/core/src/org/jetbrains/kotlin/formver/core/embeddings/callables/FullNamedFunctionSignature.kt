/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.conversion.stdLibPostconditions
import org.jetbrains.kotlin.formver.core.conversion.stdLibPreconditions
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildFunctionPretype
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.embeddings.types.nullableAny
import org.jetbrains.kotlin.formver.core.linearization.pureToViper
import org.jetbrains.kotlin.formver.core.names.DispatchReceiverName
import org.jetbrains.kotlin.formver.core.names.PlaceholderReturnVariableName
import org.jetbrains.kotlin.formver.core.purity.preorder
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.*
import org.jetbrains.kotlin.utils.addIfNotNull

interface FullNamedFunctionSignature : NamedFunctionSignature {
    /**
     * Preconditions of function in form of `ExpEmbedding`s with type `boolType()`.
     */
    val preconditions : List<ExpEmbedding>

    /**
     * Postconditions of function in form of `ExpEmbedding`s with type `boolType()`.
     */
    val postconditions : List<ExpEmbedding>

    val declarationSource: KtSourceElement?
}


class ConstructorSignature(
    val signature : NamedFunctionSignature,
    val propertiesPostconditions : List<ExpEmbedding>,
    override val symbol: FirFunctionSymbol<*>,
    val typeResolver: TypeResolver
) : FullNamedFunctionSignature, NamedFunctionSignature by signature {

   override val preconditions = buildList {
        formalArgs.forEach {
            addAll(it.pureInvariants())
            addAll(it.accessInvariants(typeResolver))
            addAll(it.provenInvariants())
            if (it.isUnique) {
                addIfNotNull(it.type.uniquePredicateAccessInvariant(typeResolver)?.fillHole(it))
            }
        }
        addAll(stdLibPreconditions(typeResolver))
    }

    override val postconditions: List<ExpEmbedding> = buildList {
        formalArgs.forEach {
            addAll(it.accessInvariants(typeResolver))
            if (it.isUnique && it.isBorrowed) {
                addIfNotNull(it.type.uniquePredicateAccessInvariant(typeResolver)?.fillHole(it))
            }
        }
        addAll(signature.returns.pureInvariants())
        addAll(signature.returns.provenInvariants())

        addAll(signature.returns.allAccessInvariants(typeResolver))
        addIfNotNull(signature.returns.uniquePredicateAccessInvariant(typeResolver))

        addAll(stdLibPostconditions(signature.returns, typeResolver))
        addAll(propertiesPostconditions)
    }

    override val declarationSource: KtSourceElement? = symbol.source
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
    override val preconditions = emptyList<ExpEmbedding>()
    override val postconditions = emptyList<ExpEmbedding>()
    override val dispatchReceiver: VariableEmbedding
        get() = PlaceholderVariableEmbedding(DispatchReceiverName, buildType { nullableAny() })
    override val extensionReceiver = null
    override val declarationSource: KtSourceElement? = propertySymbol.source

    override val returns: VariableEmbedding = PlaceholderVariableEmbedding(PlaceholderReturnVariableName, buildType { nullableAny() })
}

class GetterFunctionSignature(name: SymbolicName, symbol: FirPropertySymbol) :
    PropertyAccessorFunctionSignature(name, symbol) {
    override val symbol: FirFunctionSymbol<*>
        get() = error {
            "Getter symbol should not be accessed directly as it is allowed to be null in some cases."
        }
    override val callableType: FunctionTypeEmbedding = buildFunctionPretype {
        withDispatchReceiver { nullableAny() }
        withReturnType { nullableAny() }
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
    ctx: TypeResolver,
) = UserMethod(
    name,
    formalArgs.map { it.toLocalVarDecl() },
    returns.toLocalVarDecl(),
    preconditions.pureToViper(toBuiltin = true, ctx),
    postconditions.pureToViper(toBuiltin = true, ctx),
    body,
    declarationSource.asPosition
)

fun FullNamedFunctionSignature.toViperFunction(
    ctx: TypeResolver,
    body: Exp?,
): UserFunction {
    postconditions.forEach { postcondition ->
        val isValid = postcondition.preorder().all { it.first !is AccEmbedding && it.first !is PredicateAccessPermissions}
        if (!isValid) throw SnaktInternalException(
            declarationSource,
            "Postcondition tries to acquire permissions, which is not allowed in a function"
        )
    }
    val preconditions = formalArgs.mapNotNull { it.sharedPredicateAccessInvariant(ctx) } + preconditions
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
