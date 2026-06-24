/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.core.domains.FunctionBuilder
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings
import org.jetbrains.kotlin.formver.core.names.DomainAssociatedFuncName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Function
import org.jetbrains.kotlin.formver.viper.ast.Type

val seqToMultisetName: SymbolicName = DomainAssociatedFuncName("seqToMultiset")


val seqToMultisetFunc: Function = FunctionBuilder.build(seqToMultisetName) {
    val s = argument(Type.Seq(Type.Int))
    returns(Type.Multiset(Type.Int))
    body {
        Exp.TernaryExp(
            Exp.EqCmp(Exp.SeqLength(s), Exp.IntLit(0)),
            Exp.EmptyMultiset(Type.Int),
            Exp.AnySetUnion(
                Exp.ExplicitMultiset(listOf(Exp.SeqIndex(s, Exp.IntLit(0)))),
                Exp.FuncApp(
                    seqToMultisetName,
                    listOf(Exp.SeqDrop(s, Exp.IntLit(1))),
                    Type.Multiset(Type.Int),
                ),
            ),
        )
    }
}

object SpecialFunctions {
    val all
        get() = OperatorExpEmbeddings.allTemplates.map { it.refsOperation } + listOf(seqToMultisetFunc)
}
