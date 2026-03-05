/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.core.conversion.Havoc
import org.jetbrains.kotlin.formver.viper.ast.Method

object SpecialMethods {
    private val havoc = Havoc.getPrimitiveMethod()
    val all: List<Method> = listOf(havoc)
}