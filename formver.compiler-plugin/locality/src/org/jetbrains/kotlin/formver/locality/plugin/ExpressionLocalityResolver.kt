/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.locality.contract.plugin.resolveLocalityContract
import org.jetbrains.kotlin.formver.type.plugin.CallParametersTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.ExpressionTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.InvokeParameterTypesResolver
import org.jetbrains.kotlin.formver.type.plugin.ReturnResultTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.ThrowExceptionTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.UnifyingExpressionTypeResolver

private object TerminalLocalityResolver : ExpressionTypeResolver<Locality> {
    context(context: CheckerContext)
    override fun resolveTypeOf(expression: FirExpression): Locality =
        when (expression) {
            is FirQualifiedAccessExpression ->
                when (val symbol = expression.calleeReference.symbol) {
                    is FirVariableSymbol -> symbol.resolveLocality()
                    is FirReceiverParameterSymbol -> symbol.resolveLocality()
                    else -> null
                }

            else -> null
        }
}

class ExpressionLocalityResolver(session: FirSession) :
    ExpressionTypeResolver<Locality> by UnifyingExpressionTypeResolver(
        session.firCachesFactory,
        LocalityUnifier,
        TerminalLocalityResolver
    ), FirExtensionSessionComponent(session) {
    companion object : ExpressionTypeResolver<Locality> {
        fun getFactory(): Factory {
            return Factory { session -> ExpressionLocalityResolver(session) }
        }

        context(context: CheckerContext)
        override fun resolveTypeOf(expression: FirExpression): Locality =
            context.session.expressionLocalityResolver.resolveTypeOf(expression)
    }
}

private val FirSession.expressionLocalityResolver: ExpressionLocalityResolver
        by FirSession.sessionComponentAccessor()

context(context: CheckerContext)
fun FirExpression.resolveLocality(): Locality =
    ExpressionLocalityResolver.resolveTypeOf(this)

object ReturnResultLocalityResolver : ReturnResultTypeResolver<Locality> {
    context(context: CheckerContext)
    override fun resolveResultTypeOf(expression: FirReturnExpression): Locality = null
}

object ThrowExceptionLocalityResolver : ThrowExceptionTypeResolver<Locality> {
    context(context: CheckerContext)
    override fun resolveExceptionTypeOf(expression: FirThrowExpression): Locality = null
}

object InvokeParametersLocalityResolver : InvokeParameterTypesResolver<Locality> {
    context(context: CheckerContext)
    override fun resolveInvokeParameters(receiver: FirExpression): List<Locality>? =
        receiver.resolveLocalityContract()?.parameters?.map { it.type }
}

val CallParametersLocalityResolver = CallParametersTypeResolver(
    VariableLocalityResolver,
    InvokeParametersLocalityResolver
)
