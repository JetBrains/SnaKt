/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.orEmpty

/**
 * Resolves the types of the invoke parameters from the receiver expression.
 */
fun interface InvokeParameterTypesResolver<Type> {
    context(context: CheckerContext)
    fun resolveInvokeParameters(receiver: FirExpression): List<Type>?
}

/**
 * Resolves the types of the parameters of a call.
 *
 * @param Type the type class of the parameters.
 * @param parameterDeclaredTypeResolver the resolver for resolving the declared type of a call parameter.
 * @param invokeParameterTypesResolver the resolver for resolving the types of the parameters of an invoke call.
 */
class CallParametersTypeResolver<Type>(
    private val parameterDeclaredTypeResolver: SymbolTypeResolver<Type, FirValueParameterSymbol>,
    private val invokeParameterTypesResolver: InvokeParameterTypesResolver<Type>
) {
    private val FirCall.invokeDispatchReceiver: FirExpression?
        get() =
            when (this) {
                is FirImplicitInvokeCall -> dispatchReceiver
                is FirFunctionCall if calleeReference.name == OperatorNameConventions.INVOKE -> dispatchReceiver
                else -> null
            }

    context(_: CheckerContext)
    fun resolveParameterTypesOf(call: FirCall): List<Pair<FirExpression, Type>> {
        val invokeReceiver = call.invokeDispatchReceiver

        if (invokeReceiver != null) {
            val argumentTypes = invokeParameterTypesResolver.resolveInvokeParameters(invokeReceiver)

            if (argumentTypes != null) {
                return call.arguments.zip(argumentTypes)
            }
        }

        return call.resolvedArgumentMapping?.map { (argument, parameter) ->
            argument to parameterDeclaredTypeResolver.resolveTypeOf(parameter.symbol)
        }.orEmpty()
    }
}
