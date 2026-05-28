/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.core.conversion.IntArrayElement
import org.jetbrains.kotlin.formver.core.domains.FunctionBuilder
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings
import org.jetbrains.kotlin.formver.core.embeddings.types.IntArrayTypeEmbedding
import org.jetbrains.kotlin.formver.core.names.UnqualifiedDomainFuncName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Type


object SpecialFunctions {
    val all
        get() = OperatorExpEmbeddings.allTemplates.map { it.refsOperation } + listOf(intArrayToMultisetFunction)
}


val intArrayToMultiset = FunctionBuilder.build(UnqualifiedDomainFuncName("toMultisetInternal")) {
    // intArray
    argument {
        Type.Ref
    }
    // start Index
    argument {
        Type.Int
    }
    // end Index
    argument {
        Type.Int
    }
    returns {
        Type.Multiset(Type.Int)
    }
    val pred = Exp.PredicateAccess(IntArrayTypeEmbedding.uniquePredicateName, listOf(args[0]), PermExp.WildcardPerm())
    precondition {
        pred
    }

    precondition {
        RuntimeTypeDomain.isSubtype(RuntimeTypeDomain.typeOf(args[0]), RuntimeTypeDomain.intArrayType())
    }

    precondition {
        Exp.LeCmp(Exp.IntLit(0), args[1])
    }

    precondition {
        Exp.LeCmp(args[1], args[2])
    }
    precondition {
        Exp.LeCmp(args[2], RuntimeTypeDomain.intArrayLength(args[0]))
    }

    body {
        Exp.Unfolding(
            pred,
            Exp.TernaryExp(
                Exp.EqCmp(args[1], args[2]), Exp.EmptyMultiset(Type.Int), Exp.Union(
                    Exp.FuncApp(
                        UnqualifiedDomainFuncName("toMultisetInternal"), listOf(
                            args[0], args[1], Exp.Sub(args[2], Exp.IntLit(1))
                        ), Type.Multiset(Type.Int)
                    ),
                    Exp.ExplicitMultiset(
                        listOf(
                            RuntimeTypeDomain.intInjection.fromRef(
                                Exp.FieldAccess(
                                    Exp.FuncApp(
                                        OperatorExpEmbeddings.getSlot.refsOperation.name,
                                        listOf(
                                            args[0],
                                            RuntimeTypeDomain.intInjection.toRef(Exp.Sub(args[2], Exp.IntLit(1)))
                                        ),
                                        Type.Ref
                                    ),
                                    IntArrayElement.toViper()
                                )
                            )
                        )
                    ),
                )
            )
        )

    }
}

val intArrayToMultisetFunction = intArrayToMultiset.first
val intArrayToMultisetSignature = intArrayToMultiset.second
