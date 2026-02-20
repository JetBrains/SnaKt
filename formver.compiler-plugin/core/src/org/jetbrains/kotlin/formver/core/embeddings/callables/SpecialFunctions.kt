/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables


import org.jetbrains.kotlin.formver.core.domains.FunctionBuilder
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings
import org.jetbrains.kotlin.formver.viper.ast.Type

object SpecialFunctions {

    private val havoc =
        FunctionBuilder.build("havoc") {
            val t = argument( RuntimeTypeDomain.RuntimeType)
            returns(Type.Ref)
            postcondition { RuntimeTypeDomain.isSubtype(RuntimeTypeDomain.typeOf(result), t) }
        }
    val all
        get() = OperatorExpEmbeddings.allTemplates.map { it.refsOperation } + havoc
}
