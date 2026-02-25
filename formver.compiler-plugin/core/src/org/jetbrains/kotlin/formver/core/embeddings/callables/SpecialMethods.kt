/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.names.PlaceholderReturnVariableName
import org.jetbrains.kotlin.formver.core.names.SpecialName
import org.jetbrains.kotlin.formver.viper.ast.BuiltInMethod
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Method
import org.jetbrains.kotlin.formver.viper.ast.Type

object SpecialMethods {

    // TODO: Remove this code duplication.
    private val havocInt = BuiltInMethod(
        SpecialName("havocInt"),
        emptyList(),
        Declaration.LocalVarDecl(PlaceholderReturnVariableName, Type.Ref),
        emptyList(),
        listOf(
            RuntimeTypeDomain.isSubtype(
                RuntimeTypeDomain.typeOf(Exp.LocalVar(PlaceholderReturnVariableName, Type.Ref)),
                RuntimeTypeDomain.intType()
            ),
        ),
        null
    )

    private val havocBool = BuiltInMethod(
        SpecialName("havocBool"),
        emptyList(),
        Declaration.LocalVarDecl(PlaceholderReturnVariableName, Type.Ref),
        emptyList(),
        listOf(
            RuntimeTypeDomain.isSubtype(
                RuntimeTypeDomain.typeOf(Exp.LocalVar(PlaceholderReturnVariableName, Type.Ref)),
                RuntimeTypeDomain.intType()
            ),
        ),
        null
    )

    private val havocString = BuiltInMethod(
        SpecialName("havocString"),
        emptyList(),
        Declaration.LocalVarDecl(PlaceholderReturnVariableName, Type.Ref),
        emptyList(),
        listOf(
            RuntimeTypeDomain.isSubtype(
                RuntimeTypeDomain.typeOf(Exp.LocalVar(PlaceholderReturnVariableName, Type.Ref)),
                RuntimeTypeDomain.intType()
            ),
        ),
        null
    )

    val all: List<Method> = listOf(havocInt, havocBool, havocString)
}