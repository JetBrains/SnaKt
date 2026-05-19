/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.core.domains.MethodBuilder
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.names.DomainAssociatedFuncName
import org.jetbrains.kotlin.formver.viper.ast.BuiltInMethod

object SpecialMethods {
    val havocMethod = MethodBuilder.build(DomainAssociatedFuncName("havoc")) {
        argument {
            RuntimeTypeDomain.RuntimeType
        }

        returns {
            RuntimeTypeDomain.Ref
        }

        postcondition {
            RuntimeTypeDomain.isSubtype(RuntimeTypeDomain.typeOf(result.use()), args[0])
        }
    }
    val all: List<BuiltInMethod> = listOf(havocMethod)
}
