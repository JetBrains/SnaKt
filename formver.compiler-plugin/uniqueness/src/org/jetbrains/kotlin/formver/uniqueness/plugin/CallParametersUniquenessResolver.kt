/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.formver.type.plugin.CallParametersTypeResolver
import org.jetbrains.kotlin.formver.type.plugin.InvokeParameterTypesResolver

private object InvokeParametersUniquenessResolver : InvokeParameterTypesResolver<Uniqueness> {
    context(context: CheckerContext)
    override fun resolveInvokeParameters(receiver: FirExpression): List<Uniqueness>? =
        null
}

val CallParametersUniquenessResolver = CallParametersTypeResolver(
    VariableUniquenessResolver,
    InvokeParametersUniquenessResolver
)
