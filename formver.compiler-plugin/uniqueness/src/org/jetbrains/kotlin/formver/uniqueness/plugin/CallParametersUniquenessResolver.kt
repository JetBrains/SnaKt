/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.formver.type.plugin.CallArgumentTypeFactsMapper
import org.jetbrains.kotlin.formver.type.plugin.InvokeParameterTypeFactsResolver

private object InvokeParametersUniquenessResolver : InvokeParameterTypeFactsResolver<Uniqueness> {
    context(context: CheckerContext)
    override fun resolveInvokeParameters(receiver: FirExpression): List<Uniqueness>? =
        null // TODO: Implement uniqueness contract resolution
}

val CallParametersUniquenessResolver = CallArgumentTypeFactsMapper(
    ParameterUniquenessResolver,
    InvokeParametersUniquenessResolver
)
