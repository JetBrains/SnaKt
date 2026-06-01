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
 * Resolves the type of an expression.
 */
fun interface ExpressionTypeResolver<Type> {
    context(context: CheckerContext)
    fun resolveTypeOf(expression: FirExpression): Type
}

/**
 * Resolves the type of the result of a return expression.
 */
fun interface ReturnResultTypeResolver<Type> {
    context(context: CheckerContext)
    fun resolveResultTypeOf(expression: FirReturnExpression): Type
}

/**
 * Resolves the expected type of the exception in throw expression.
 */
fun interface ThrowExceptionTypeResolver<Type> {
    context(context: CheckerContext)
    fun resolveExceptionTypeOf(expression: FirThrowExpression): Type
}
