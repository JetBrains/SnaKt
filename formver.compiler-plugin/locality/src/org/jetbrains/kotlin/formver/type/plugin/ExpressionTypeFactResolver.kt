/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.type.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression

/**
 * Resolves the type fact of an expression.
 */
fun interface ExpressionTypeFactResolver<TypeFact> {
    context(context: CheckerContext)
    fun resolveTypeFactOf(expression: FirExpression): TypeFact
}

/**
 * Resolves the type fact of the result of a return expression.
 */
fun interface ReturnResultTypeFactResolver<TypeFact> {
    context(context: CheckerContext)
    fun resolveResultTypeFactOf(expression: FirReturnExpression): TypeFact
}

/**
 * Resolves the expected type fact of the exception in throw expression.
 */
fun interface ThrowExceptionTypeFactResolver<TypeFact> {
    context(context: CheckerContext)
    fun resolveExceptionTypeFactOf(expression: FirThrowExpression): TypeFact
}
