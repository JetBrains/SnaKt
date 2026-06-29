/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.core.conversion.ArrayCellDataFieldEmbedding
import org.jetbrains.kotlin.formver.core.domains.FunctionBuilder
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings
import org.jetbrains.kotlin.formver.core.embeddings.types.IntArrayTypeEmbedding
import org.jetbrains.kotlin.formver.core.names.DomainAssociatedFuncName
import org.jetbrains.kotlin.formver.core.names.DomainFuncParameterName
import org.jetbrains.kotlin.formver.viper.ast.*
import org.jetbrains.kotlin.formver.viper.ast.Exp.Companion.toConjunction


object SpecialFunctions {
    private val slotsToMultisetName = DomainAssociatedFuncName("slotsToMultiset")
    private val arrayToMultisetName = DomainAssociatedFuncName("arrayToMultiset")
    val slotsToMultisetFunction: BuiltinFunction = FunctionBuilder.build(slotsToMultisetName) {
        val arr = argument(Type.Ref)
        val start = argument(Type.Int)
        val end = argument(Type.Int)
        returns(Type.Multiset(Type.Int))

        precondition {
            listOf(
                0.toExp() le start,
                start le end,
                end le RuntimeTypeDomain.size(arr)
            ).toConjunction()
        }

        // forall j :: { slot(arr,j).array_cell_int } (start <= j && j < end) ==> acc(slot(arr,j).array_cell_int, write)
        val jVar = Var(DomainFuncParameterName("j"), Type.Int)
        precondition {
            Exp.forall(jVar) { j ->
                assumption { (start le j) and (j lt end) }
                val slotApp = Exp.DomainFuncApp(RuntimeTypeDomain.slot, listOf(arr, j), emptyMap())
                val fieldAcc = Exp.FieldAccess(slotApp, ArrayCellDataFieldEmbedding.toViper())
                simpleTrigger { fieldAcc }
                Exp.Acc(fieldAcc, PermExp.FullPerm())
            }
        }

        postcondition {
            Exp.AnySetCardinality(result) eq (end - start)
        }

        body {
            val startSlotFieldAccess =
                Exp.FieldAccess(RuntimeTypeDomain.slot(arr, start), ArrayCellDataFieldEmbedding.toViper())
            val startElemInt = RuntimeTypeDomain.intInjection.fromRef(startSlotFieldAccess)
            Exp.TernaryExp(
                start eq end,
                Exp.EmptyMultiset(Type.Int),
                Exp.AnySetUnion(
                    Exp.ExplicitMultiset(listOf(startElemInt)),
                    Exp.FuncApp(slotsToMultisetName, listOf(arr, start + 1.toExp(), end), Type.Multiset(Type.Int))
                )
            )
        }
    }

    val arrayToMultisetFunction: BuiltinFunction = FunctionBuilder.build(arrayToMultisetName) {
        val arr = argument(Type.Ref)
        returns(Type.Multiset(Type.Int))

        precondition {
            Exp.PredicateAccess(IntArrayTypeEmbedding.uniquePredicateName, listOf(arr), PermExp.FullPerm())
        }

        postcondition {
            Exp.AnySetCardinality(result) eq RuntimeTypeDomain.size(arr)
        }

        body {
            Exp.Unfolding(
                Exp.PredicateAccess(IntArrayTypeEmbedding.uniquePredicateName, listOf(arr), PermExp.WildcardPerm()),
                Exp.FuncApp(
                    slotsToMultisetName,
                    listOf(arr, 0.toExp(), RuntimeTypeDomain.size(arr)),
                    Type.Multiset(Type.Int)
                )
            )
        }
    }


    val all
        get() = OperatorExpEmbeddings.allTemplates.map { it.refsOperation } +
                listOf(slotsToMultisetFunction, arrayToMultisetFunction)
}
