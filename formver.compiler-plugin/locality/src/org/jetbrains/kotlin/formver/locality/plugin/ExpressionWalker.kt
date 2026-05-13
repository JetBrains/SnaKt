/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.expressions.unwrapExpression
import org.jetbrains.kotlin.fir.lastExpression
import org.jetbrains.kotlin.utils.yieldIfNotNull

fun FirExpression.removeCast(): FirExpression =
    when (this) {
        is FirTypeOperatorCall -> when (operation) {
            FirOperation.AS, FirOperation.SAFE_AS ->
                argument.unwrapExpression().removeCast()
            else -> this
        }
        else -> this
    }

fun FirExpression.collectTails(): Sequence<FirExpression> =
    when (val expression = unwrapExpression()) {
        is FirWhenExpression ->
            sequence {
                yieldAll(expression.branches.map(FirWhenBranch::result))
            }
        is FirTryExpression ->
            sequence {
                yield(expression.tryBlock)
                yieldAll(expression.catches.map(FirCatch::block))
            }
        is FirBlock ->
            sequence {
                yieldIfNotNull(expression.lastExpression)
            }
        else ->
            emptySequence()
    }
