/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.properties

import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.callables.GenericFunctionSignatureMixin
import org.jetbrains.kotlin.formver.core.embeddings.callables.NamedFunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings
import org.jetbrains.kotlin.formver.core.embeddings.expression.PlaceholderVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildFunctionPretype
import org.jetbrains.kotlin.formver.core.names.FunctionResultVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName

object LengthFieldGetter : GetterEmbedding {
    override fun getValue(receiver: ExpEmbedding, ctx: TypeResolver) =
        OperatorExpEmbeddings.StringLength(receiver)

    override fun getValueSimple(
        receiver: ExpEmbedding,
        ctx: TypeResolver
    ): ExpEmbedding = OperatorExpEmbeddings.StringLength(receiver)
}


object IntArraySizeGetter : GetterEmbedding {
    val callable = buildFunctionPretype {
        withDispatchReceiver {
            intArray()
        }
        withReturnType {
            int()
        }
    }

    val signature: NamedFunctionSignature = object : NamedFunctionSignature, GenericFunctionSignatureMixin() {
        override val name: SymbolicName = RuntimeTypeDomain.intArrayLength.name
        override val symbol: FirFunctionSymbol<*>
            get() = error("should not be accessed")
        override val callableType: FunctionTypeEmbedding
            get() = callable
        override val returns: VariableEmbedding
            get() = PlaceholderVariableEmbedding(FunctionResultVariableName, callable.returnType)
        override val isPure: Boolean = true
    }


    override fun getValue(receiver: ExpEmbedding, ctx: TypeResolver) =
        OperatorExpEmbeddings.intArraySize(receiver)

    override fun getValueSimple(
        receiver: ExpEmbedding,
        ctx: TypeResolver
    ): ExpEmbedding = OperatorExpEmbeddings.intArraySize(receiver)
}
