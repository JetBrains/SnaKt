/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.core.embeddings.FunctionBodyEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.withIsUnitInvariantIfUnit
import org.jetbrains.kotlin.formver.core.linearization.*
import org.jetbrains.kotlin.formver.viper.ast.Exp

fun ProgramConversionContext.linearizeImpureBody(
    source: KtSourceElement?,
    converted: ConvertedMethodBody,
): FunctionBodyEmbedding {
    val seqnBuilder = SeqnBuilder(source)
    val linearizer =
        Linearizer(SharedLinearizationState(anonVarProducer), seqnBuilder, source, typeResolver)
    converted.bodyExp.toLinearizable(source).toViperUnusedResult(linearizer)
    // A Unit body may fall through without an explicit return. Inhale its result's Unit subtype invariant;
    // non-Unit results are unchanged.
    converted.returnTarget.variable.withIsUnitInvariantIfUnit(typeResolver)
        .toLinearizable(source).toViperUnusedResult(linearizer)
    return FunctionBodyEmbedding(seqnBuilder.block)
}

fun ProgramConversionContext.linearizePureBody(
    source: KtSourceElement?,
    body: ExpEmbedding,
): Exp {
    val pureFunBodyLinearizer = PureFunBodyLinearizer(
        source,
        SharedLinearizationState(anonVarProducer),
        SsaConverter(source),
        typeResolver
    )
    body.toLinearizable(source).toViperUnusedResult(pureFunBodyLinearizer)
    return pureFunBodyLinearizer.constructExpression()
}
