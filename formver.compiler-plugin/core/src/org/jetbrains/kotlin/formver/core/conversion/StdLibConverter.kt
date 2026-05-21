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
import org.jetbrains.kotlin.formver.core.embeddings.properties.IntArrayProperty
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.names.AnonymousBuiltinName
import org.jetbrains.kotlin.formver.core.names.ClassScopeNameMatcher
import org.jetbrains.kotlin.formver.core.names.NameMatcher
import org.jetbrains.kotlin.formver.core.names.NameType

private fun VariableEmbedding.sameSize(): ExpEmbedding =
    EqCmp(FieldAccess(this, CollectionSizeFieldEmbedding), Old(FieldAccess(this, CollectionSizeFieldEmbedding)))

private fun VariableEmbedding.increasedSize(amount: Int): ExpEmbedding = EqCmp(
    FieldAccess(this, CollectionSizeFieldEmbedding),
    OperatorExpEmbeddings.AddIntInt(Old(FieldAccess(this, CollectionSizeFieldEmbedding)), IntLit(amount)),
)

sealed interface StdLibReceiverInterface {
    fun match(function: NamedFunctionSignature, ctx: TypeResolver): Boolean
}

sealed interface StdLibFunctionMatcher {
    fun match(function: NamedFunctionSignature, ctx: TypeResolver): Boolean
}

data object ConstructorMatcher : StdLibFunctionMatcher {
    override fun match(
        function: NamedFunctionSignature, ctx: TypeResolver
    ): Boolean = function.name.nameType == NameType.Base.Constructor
}

data object IntArrayConstructorMatcher : StdLibFunctionMatcher {
    override fun match(
        function: NamedFunctionSignature, ctx: TypeResolver
    ): Boolean {

        ClassScopeNameMatcher(function.name).ifConstructorName {
            ifClassName("IntArray") {
                return true
            }
        }
        return false
    }
}

sealed interface PresentInterface : StdLibReceiverInterface {
    val interfaceName: String
    override fun match(function: NamedFunctionSignature, ctx: TypeResolver): Boolean =
        function.callableType.dispatchReceiverType?.pretype?.let {
            ctx.isInheritorOfCollectionTypeNamed(
                it, interfaceName
            )
        } ?: false
}

data object CollectionInterface : PresentInterface {
    override val interfaceName = "Collection"
}

data object ListInterface : PresentInterface {
    override val interfaceName = "List"
}

data object MutableListInterface : PresentInterface {
    override val interfaceName = "MutableList"
}

data object NoInterface : StdLibReceiverInterface {
    override fun match(function: NamedFunctionSignature, ctx: TypeResolver): Boolean =
        NameMatcher.matchClassScope(function.name) {
            ifInCollectionsPkg {
                ifNoReceiver {
                    return true
                }
            }
            return false
        }
}

sealed interface StdLibCondition {
    val stdLibInterface: StdLibReceiverInterface
    val functionName: String

    fun match(function: NamedFunctionSignature): Boolean {
        NameMatcher.matchClassScope(function.name) {
            ifFunctionName(functionName) {
                return true
            }
            return false
        }
    }
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
            EmptyListPostcondition, IsEmptyPostcondition, GetPostcondition, SubListPostcondition, AddPostcondition
        )
    }

    fun getEmbeddings(
        returnVariable: VariableEmbedding, function: NamedFunctionSignature, ctx: TypeResolver
    ): List<ExpEmbedding>
}

data object GetPrecondition : StdLibPrecondition {
    override fun getEmbeddings(function: NamedFunctionSignature): List<ExpEmbedding> {
        val receiver = function.dispatchReceiver!!
        val indexArg = function.formalArgs[1]
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

    override val stdLibInterface = ListInterface
    override val functionName = "get"
}

data object SubListPrecondition : StdLibPrecondition {
    override fun getEmbeddings(function: NamedFunctionSignature): List<ExpEmbedding> {
        val receiver = function.dispatchReceiver!!
        val fromIndexArg = function.formalArgs[1]
        val toIndexArg = function.formalArgs[2]
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

    override val stdLibInterface = ListInterface
    override val functionName = "subList"
}

data object EmptyListPostcondition : StdLibPostcondition {
    override fun getEmbeddings(
        returnVariable: VariableEmbedding, function: NamedFunctionSignature, ctx: TypeResolver
    ): List<ExpEmbedding> {
        return listOf(
            EqCmp(FieldAccess(returnVariable, CollectionSizeFieldEmbedding), IntLit(0))
        )
    }

    override val stdLibInterface = NoInterface
    override val functionName = "emptyList"
}

data object IsEmptyPostcondition : StdLibPostcondition {
    override fun getEmbeddings(
        returnVariable: VariableEmbedding, function: NamedFunctionSignature, ctx: TypeResolver
    ): List<ExpEmbedding> {
        val receiver = function.dispatchReceiver!!
        return listOf(
            receiver.sameSize(),
            Implies(returnVariable, EqCmp(FieldAccess(receiver, CollectionSizeFieldEmbedding), IntLit(0))),
            Implies(Not(returnVariable), GtIntInt(FieldAccess(receiver, CollectionSizeFieldEmbedding), IntLit(0)))
        )
    }

    override val stdLibInterface = CollectionInterface
    override val functionName = "isEmpty"
}

data object GetPostcondition : StdLibPostcondition {
    override fun getEmbeddings(
        returnVariable: VariableEmbedding, function: NamedFunctionSignature, ctx: TypeResolver
    ): List<ExpEmbedding> {
        return listOf(function.dispatchReceiver!!.sameSize())
    }

    override val stdLibInterface = ListInterface
    override val functionName = "get"
}

data object SubListPostcondition : StdLibPostcondition {
    override fun getEmbeddings(
        returnVariable: VariableEmbedding, function: NamedFunctionSignature, ctx: TypeResolver
    ): List<ExpEmbedding> {
        val fromIndexArg = function.formalArgs[1]
        val toIndexArg = function.formalArgs[2]
        return listOf(
            function.dispatchReceiver!!.sameSize(),
            EqCmp(FieldAccess(returnVariable, CollectionSizeFieldEmbedding), SubIntInt(toIndexArg, fromIndexArg))
        )
    }

    override val stdLibInterface = ListInterface
    override val functionName = "subList"
}

data object AddPostcondition : StdLibPostcondition {
    override fun getEmbeddings(
        returnVariable: VariableEmbedding, function: NamedFunctionSignature, ctx: TypeResolver
    ): List<ExpEmbedding> {
        return listOf(function.dispatchReceiver!!.increasedSize(1))
    }

    override val stdLibInterface = MutableListInterface
    override val functionName = "add"
}


data object IntArrayLengthConstructor {

    fun getPreconditions(
        function: NamedFunctionSignature, ctx: TypeResolver
    ): List<ExpEmbedding> {
        if (!condition.match(function, ctx)) return listOf()
        return listOf(
            GeIntInt(
                function.formalArgs[0],
                IntLit(0),
            )
        )
    }


    fun getPostconditions(
        returnVariable: VariableEmbedding, function: NamedFunctionSignature, ctx: TypeResolver
    ): List<ExpEmbedding> {
        if (!condition.match(function, ctx)) return listOf()

        val forallVariable = PlaceholderVariableEmbedding(AnonymousBuiltinName(0), buildType { int() })
//        ctx.freshAnonBuiltinVar(buildType { int() })
        return listOf(
            EqCmp(Size(IntArrayProperty.property.getter!!.getValue(returnVariable, ctx)), function.formalArgs[0]),
            ForAllEmbedding(
                forallVariable, listOf(

                    Implies(
                        OperatorExpEmbeddings.And(

                            GeIntInt(
                                forallVariable, IntLit(0)
                            ), OperatorExpEmbeddings.LtIntInt(
                                forallVariable, Size(IntArrayProperty.property.getter.getValue(returnVariable, ctx))
                            )
                        ), EqCmp(
                            SeqLookup(IntArrayProperty.property.getter.getValue(returnVariable, ctx), forallVariable),
                            IntLit(0)
                        )
                    )
                )
            )
        )
    }

    val condition = IntArrayConstructorMatcher


}

fun NamedFunctionSignature.stdLibPreconditions(ctx: TypeResolver): List<ExpEmbedding> {
    val precon = StdLibPrecondition.all.groupBy {
        it.stdLibInterface
    }.flatMap { (stdLibInterface, preconditions) ->
        if (stdLibInterface.match(this, ctx)) {
            preconditions.flatMap {
                if (it.match(this)) {
                    it.getEmbeddings(this)
                } else {
                    emptyList()
                }
            }
        } else {
            emptyList()
        }
    }
    val functionPrecon = listOf(IntArrayLengthConstructor).flatMap {
        it.getPreconditions(this, ctx)
    }

    return precon + functionPrecon
}


fun NamedFunctionSignature.stdLibPostconditions(
    returnVariable: VariableEmbedding, ctx: TypeResolver
): List<ExpEmbedding> {
    val postcon = StdLibPostcondition.all.groupBy {
        it.stdLibInterface
    }.flatMap { (stdLibInterface, postconditions) ->
        if (stdLibInterface.match(this, ctx)) {
            postconditions.flatMap {
                if (it.match(this)) {
                    it.getEmbeddings(returnVariable, this, ctx)
                } else {
                    emptyList()
                }
            }
        } else {
            emptyList()
        }
    }

    val functionPostcon = listOf(IntArrayLengthConstructor).flatMap {
        it.getPostconditions(returnVariable, this, ctx)
    }


    return postcon + functionPostcon
}
