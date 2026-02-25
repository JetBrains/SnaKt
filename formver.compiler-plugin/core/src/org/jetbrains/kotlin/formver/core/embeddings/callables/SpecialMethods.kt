/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.names.PlaceholderArgumentName
import org.jetbrains.kotlin.formver.core.names.PlaceholderReturnVariableName
import org.jetbrains.kotlin.formver.core.names.SpecialName
import org.jetbrains.kotlin.formver.viper.ast.BuiltInMethod
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Method
import org.jetbrains.kotlin.formver.viper.ast.Type

object SpecialMethods {

    private val havoc = BuiltInMethod(
            SpecialName("havoc"),
            listOf(Declaration.LocalVarDecl(PlaceholderArgumentName(0), RuntimeTypeDomain.RuntimeType)),
            Declaration.LocalVarDecl(PlaceholderReturnVariableName, Type.Ref),
            emptyList(),
            listOf(
                RuntimeTypeDomain.isSubtype(
                    RuntimeTypeDomain.typeOf(Exp.LocalVar(PlaceholderReturnVariableName, Type.Ref)),
                    Exp.LocalVar(PlaceholderArgumentName(0), RuntimeTypeDomain.RuntimeType)
                ),
            ),
            null
        )

    val all: List<Method> = listOf(havoc)
}