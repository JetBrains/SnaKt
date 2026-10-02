/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.formver.core.embeddings.SourceRole
import org.jetbrains.kotlin.formver.core.embeddings.callables.NamedFunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.GeIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.GtIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.Implies
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.LeIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.Not
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.SubIntInt
import org.jetbrains.kotlin.formver.core.names.SpecialPackages

private fun VariableEmbedding.sameSize(): ExpEmbedding =
    EqCmp(FieldAccess(this, CollectionSizeFieldEmbedding), Old(FieldAccess(this, CollectionSizeFieldEmbedding)))

private fun VariableEmbedding.increasedSize(amount: Int): ExpEmbedding = EqCmp(
    FieldAccess(this, CollectionSizeFieldEmbedding),
    OperatorExpEmbeddings.AddIntInt(Old(FieldAccess(this, CollectionSizeFieldEmbedding)), IntLit(amount)),
)

sealed interface StdLibCondition {
    val pattern: StdLibFunctionPattern
}

sealed interface StdLibPrecondition : StdLibCondition {
    companion object {
        val all = listOf(GetPrecondition, SubListPrecondition)
    }

    fun getEmbeddings(function: NamedFunctionSignature): List<ExpEmbedding>
}

sealed interface StdLibPostcondition : StdLibCondition {
    companion object {
        val all = listOf(
            EmptyListPostcondition,
            IsEmptyPostcondition,
            GetPostcondition,
            SubListPostcondition,
            AddPostcondition,
        )
    }

    fun getEmbeddings(returnVariable: VariableEmbedding, function: NamedFunctionSignature): List<ExpEmbedding>
}

data object GetPrecondition : StdLibPrecondition {
    override val pattern = MemberOf(SpecialPackages.collections, "List", "get", arity = 1)

    override fun getEmbeddings(function: NamedFunctionSignature): List<ExpEmbedding> {
        val receiver = function.dispatchReceiver!!
        val indexArg = function.params[0]
        return listOf(
            GeIntInt(
                indexArg,
                IntLit(0),
                SourceRole.ListElementAccessCheck(SourceRole.ListElementAccessCheck.AccessCheckType.LESS_THAN_ZERO)
            ),
            GtIntInt(
                FieldAccess(receiver, CollectionSizeFieldEmbedding),
                indexArg,
                SourceRole.ListElementAccessCheck(SourceRole.ListElementAccessCheck.AccessCheckType.GREATER_THAN_LIST_SIZE)
            ),
        )
    }
}

data object SubListPrecondition : StdLibPrecondition {
    override val pattern = MemberOf(SpecialPackages.collections, "List", "subList", arity = 2)

    override fun getEmbeddings(function: NamedFunctionSignature): List<ExpEmbedding> {
        val receiver = function.dispatchReceiver!!
        val fromIndexArg = function.params[0]
        val toIndexArg = function.params[1]
        return listOf(
            LeIntInt(fromIndexArg, toIndexArg, SourceRole.SubListCreation.CheckInSize),
            GeIntInt(fromIndexArg, IntLit(0), SourceRole.SubListCreation.CheckNegativeIndices),
            LeIntInt(
                toIndexArg,
                FieldAccess(receiver, CollectionSizeFieldEmbedding),
                SourceRole.SubListCreation.CheckInSize
            )
        )
    }
}

data object EmptyListPostcondition : StdLibPostcondition {
    override val pattern = TopLevel(SpecialPackages.collections, "emptyList", arity = 0)

    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> = listOf(EqCmp(FieldAccess(returnVariable, CollectionSizeFieldEmbedding), IntLit(0)))
}

data object IsEmptyPostcondition : StdLibPostcondition {
    override val pattern = MemberOf(SpecialPackages.collections, "Collection", "isEmpty", arity = 0)

    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> {
        val receiver = function.dispatchReceiver!!
        return listOf(
            receiver.sameSize(),
            Implies(returnVariable, EqCmp(FieldAccess(receiver, CollectionSizeFieldEmbedding), IntLit(0))),
            Implies(Not(returnVariable), GtIntInt(FieldAccess(receiver, CollectionSizeFieldEmbedding), IntLit(0)))
        )
    }
}

data object GetPostcondition : StdLibPostcondition {
    override val pattern = MemberOf(SpecialPackages.collections, "List", "get", arity = 1)

    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> = listOf(function.dispatchReceiver!!.sameSize())
}

data object SubListPostcondition : StdLibPostcondition {
    override val pattern = MemberOf(SpecialPackages.collections, "List", "subList", arity = 2)

    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> {
        val fromIndexArg = function.params[0]
        val toIndexArg = function.params[1]
        return listOf(
            function.dispatchReceiver!!.sameSize(),
            EqCmp(FieldAccess(returnVariable, CollectionSizeFieldEmbedding), SubIntInt(toIndexArg, fromIndexArg))
        )
    }
}

data object AddPostcondition : StdLibPostcondition {
    override val pattern = MemberOf(SpecialPackages.collections, "MutableList", "add", arity = 1)

    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> = listOf(function.dispatchReceiver!!.increasedSize(1))
}

fun NamedFunctionSignature.stdLibPreconditions(ctx: TypeResolver): List<ExpEmbedding> =
    StdLibPrecondition.all.filter { it.pattern.matches(this, ctx) }.flatMap { it.getEmbeddings(this) }

fun NamedFunctionSignature.stdLibPostconditions(
    returnVariable: VariableEmbedding,
    ctx: TypeResolver,
): List<ExpEmbedding> =
    StdLibPostcondition.all.filter { it.pattern.matches(this, ctx) }.flatMap { it.getEmbeddings(returnVariable, this) }
