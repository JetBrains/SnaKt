/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.asPosition
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.expression.AccEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.PlaceholderVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.PredicateAccessPermissions
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.linearization.pureToViper
import org.jetbrains.kotlin.formver.core.names.AnonymousName
import org.jetbrains.kotlin.formver.core.names.DispatchReceiverName
import org.jetbrains.kotlin.formver.core.names.ExtensionReceiverName
import org.jetbrains.kotlin.formver.core.purity.preorder
import org.jetbrains.kotlin.formver.viper.ast.*

interface FunctionSignature {
    val callableType: FunctionTypeEmbedding
    val dispatchReceiver: VariableEmbedding?
    val extensionReceiver: VariableEmbedding?

    val params: List<VariableEmbedding>

    val returns : VariableEmbedding

    val isPure: Boolean

    val labelName: String?
        get() = null

    val formalArgs: List<VariableEmbedding>
        get() = listOfNotNull(dispatchReceiver, extensionReceiver) + params
}

data class FunctionSignatureImpl(
    override val callableType: FunctionTypeEmbedding,
    override val dispatchReceiver: VariableEmbedding?,
    override val extensionReceiver: VariableEmbedding?,
    override val params: List<VariableEmbedding>,
    override val returns: VariableEmbedding,
    override val isPure: Boolean,
    override val labelName: String? = null,
) : FunctionSignature


data class GenericFunctionSignature(
    override val callableType: FunctionTypeEmbedding,
    override val returns: VariableEmbedding,
    override val isPure: Boolean,
) : FunctionSignature, GenericFunctionSignatureMixin()

abstract class GenericFunctionSignatureMixin : FunctionSignature {
    override val dispatchReceiver: VariableEmbedding?
        get() = callableType.dispatchReceiverType?.let { PlaceholderVariableEmbedding(DispatchReceiverName, it) }

    override val extensionReceiver: VariableEmbedding?
        get() = callableType.extensionReceiverType?.let { PlaceholderVariableEmbedding(ExtensionReceiverName, it) }

    override val params: List<VariableEmbedding>
        get() = callableType.paramTypes.mapIndexed { ix, type -> PlaceholderVariableEmbedding(AnonymousName(ix), type) }
}


interface CompleteFunctionSignature : NamedFunctionSignatureWithContract, NamedCallableEmbedding

fun CompleteFunctionSignature.toViperMethod(ctx: TypeResolver, body: Stmt.Seqn?): Method {
    require(!isPure) {
        "Pure functions should not be converted to methods"
    }
    return UserMethod(
        name,
        formalArgs.map { it.toLocalVarDecl() },
        returns.toLocalVarDecl(),
        preconditions.pureToViper(toBuiltin = true, ctx),
        postconditions.pureToViper(toBuiltin = true, ctx),
        body,
        declarationSource.asPosition
    )
}

fun CompleteFunctionSignature.toViperFunction(
    ctx: TypeResolver,
    body: Exp?,
): UserFunction {
    require(isPure) {
        "Impure functions should not be converted to functions"
    }
    postconditions.forEach { postcondition ->
        val isValid =
            postcondition.preorder().all { it.first !is AccEmbedding && it.first !is PredicateAccessPermissions }
        if (!isValid) throw SnaktInternalException(
            declarationSource, "Postcondition tries to acquire permissions, which is not allowed in a function"
        )
    }
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
