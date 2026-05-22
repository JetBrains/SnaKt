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
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.kotlinCallableId
import org.jetbrains.kotlin.formver.core.kotlinClassId
import org.jetbrains.kotlin.formver.core.names.*

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

data class IntArrayAnyFunctionMatcher(val funcName: String) : StdLibFunctionMatcher {
    override fun match(
        function: NamedFunctionSignature, ctx: TypeResolver
    ): Boolean {
        NameMatcher.matchClassScope(function.name) {
            ifClassName("IntArray") {
                ifFunctionName(funcName) {
                    return true
                }
            }
            return false
        }
    }
}

data class IntArrayGetterMatcher(val fieldName: String) : StdLibFunctionMatcher {
    override fun match(
        function: NamedFunctionSignature, ctx: TypeResolver
    ): Boolean {
        NameMatcher.matchClassScope(function.name) {
            ifClassName("IntArray") {
                ifGetterName(fieldName) {
                    return true
                }
            }
            return false
        }
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
                toIndexArg, FieldAccess(receiver, CollectionSizeFieldEmbedding), SourceRole.SubListCreation.CheckInSize
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


interface StdFunctionConditions {
    fun getPreconditions(function: NamedFunctionSignature, ctx: TypeResolver): List<ExpEmbedding>
    fun getPostconditions(
        returnVariable: VariableEmbedding, function: NamedFunctionSignature, ctx: TypeResolver
    ): List<ExpEmbedding>

    val condition: StdLibFunctionMatcher
}


data object IntArrayLengthConstructor : StdFunctionConditions {

    override fun getPreconditions(
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


    override fun getPostconditions(
        returnVariable: VariableEmbedding, function: NamedFunctionSignature, ctx: TypeResolver
    ): List<ExpEmbedding> {
        if (!condition.match(function, ctx)) return listOf()
        val className = kotlinClassId("IntArray").embedName()
        val sizePropertyName =
            kotlinCallableId("IntArray", "size").embedMemberPropertyName(MemberEmbeddingPolicy.FUNCTION)
        val classTypeEmbedding = ctx.lookupClassTypeEmbedding(className)!!
        val sizeProperty = ctx.lookupProperty(ClassPropertyPair(className, sizePropertyName))
        val forallVariable = PlaceholderVariableEmbedding(AnonymousBuiltinName(0), buildType { int() })
//        ctx.freshAnonBuiltinVar(buildType { int() })
        return listOf(
            // equal to variable
            EqCmp(
                Unfolding(
                    Size(IntArrayProperty.property.getter!!.getValue(returnVariable, ctx)),
                    classTypeEmbedding.uniquePredicateAccessInvariant(ctx)
                        .fillHole(returnVariable) as PredicateAccessPermissions
                ), function.formalArgs[0]
            ),
            // equal to getter function
            EqCmp(
                Unfolding(
                    Size(IntArrayProperty.property.getter!!.getValue(returnVariable, ctx)),
                    classTypeEmbedding.uniquePredicateAccessInvariant(ctx)
                        .fillHole(returnVariable) as PredicateAccessPermissions
                ), sizeProperty!!.getter!!.getValue(returnVariable, ctx)
            ),
            Unfolding(
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
                                SeqLookup(
                                    IntArrayProperty.property.getter.getValue(returnVariable, ctx),
                                    forallVariable
                                ), IntLit(0)
                            )
                        )
                    )
                ),
                classTypeEmbedding.uniquePredicateAccessInvariant(ctx)
                    .fillHole(returnVariable) as PredicateAccessPermissions
            )
        )
    }

    override val condition = IntArrayConstructorMatcher

}

data object IntArraySizeFunction : StdFunctionConditions {

    fun permission(receiver: ExpEmbedding, ctx: TypeResolver) =
        (receiver.type.pretype as ClassTypeEmbedding).uniquePredicateAccessInvariant(ctx)
            .fillHole(receiver) as PredicateAccessPermissions


    fun wrapUnfold(predicate: PredicateAccessPermissions, inner: ExpEmbedding): ExpEmbedding =
        Unfolding(
            inner, predicate
        )

    override fun getPreconditions(
        function: NamedFunctionSignature, ctx: TypeResolver
    ): List<ExpEmbedding> {
        if (!condition.match(function, ctx)) return listOf()
        return buildList {
            val receiver = function.formalArgs[0]
            add(permission(receiver, ctx))
        }
    }

    override fun getPostconditions(
        returnVariable: VariableEmbedding, function: NamedFunctionSignature, ctx: TypeResolver
    ): List<ExpEmbedding> {
        if (!condition.match(function, ctx)) return listOf()
        val receiver = function.formalArgs[0]
        return listOf(
            EqCmp(
                Size(
                    wrapUnfold(
                        permission(receiver, ctx),
                        IntArrayProperty.property.getter!!.getValue(function.formalArgs[0], ctx)
                    ),
                ), returnVariable
            )
        )
    }

    override val condition = IntArrayGetterMatcher("size")
}

data object IntArrayGetFunction : StdFunctionConditions {

    fun permission(receiver: ExpEmbedding, ctx: TypeResolver) =
        (receiver.type.pretype as ClassTypeEmbedding).uniquePredicateAccessInvariant(ctx)
            .fillHole(receiver) as PredicateAccessPermissions


    fun wrapUnfold(predicate: PredicateAccessPermissions, inner: ExpEmbedding): ExpEmbedding =
        Unfolding(
            inner, predicate
        )

    fun boundCheck(receiver: ExpEmbedding, index: ExpEmbedding, ctx: TypeResolver) = OperatorExpEmbeddings.And(
        GeIntInt(
            index,
            IntLit(0),
        ), OperatorExpEmbeddings.LtIntInt(
            index,
            wrapUnfold(permission(receiver, ctx), Size(IntArrayProperty.property.getter!!.getValue(receiver, ctx)))
        )
    )

    override fun getPreconditions(
        function: NamedFunctionSignature, ctx: TypeResolver
    ): List<ExpEmbedding> {
        if (!condition.match(function, ctx)) return listOf()
        return buildList {
            val receiver = function.formalArgs[0]
            val index = function.formalArgs[1]

            add(permission(receiver, ctx))
            add(boundCheck(receiver, index, ctx))
        }
    }

    override fun getPostconditions(
        returnVariable: VariableEmbedding, function: NamedFunctionSignature, ctx: TypeResolver
    ): List<ExpEmbedding> {
        if (!condition.match(function, ctx)) return listOf()
        val receiver = function.formalArgs[0]
        return listOf(
            EqCmp(
                SeqLookup(
                    wrapUnfold(
                        permission(receiver, ctx),
                        IntArrayProperty.property.getter!!.getValue(function.formalArgs[0], ctx)
                    ),
                    function.formalArgs[1]
                ), returnVariable
            )
        )
    }

    override val condition = IntArrayAnyFunctionMatcher("get")
}

data object IntArraySetFunction : StdFunctionConditions {

    fun permission(receiver: ExpEmbedding, ctx: TypeResolver) =
        (receiver.type.pretype as ClassTypeEmbedding).uniquePredicateAccessInvariant(ctx)
            .fillHole(receiver) as PredicateAccessPermissions


    fun wrapUnfold(predicate: PredicateAccessPermissions, inner: ExpEmbedding): ExpEmbedding =
        Unfolding(
            inner, predicate
        )

    fun boundCheck(receiver: ExpEmbedding, index: ExpEmbedding, ctx: TypeResolver) = OperatorExpEmbeddings.And(
        GeIntInt(
            index,
            IntLit(0),
        ), OperatorExpEmbeddings.LtIntInt(
            index,
            Size(wrapUnfold(permission(receiver, ctx), IntArrayProperty.property.getter!!.getValue(receiver, ctx)))
        )
    )

    override fun getPreconditions(
        function: NamedFunctionSignature, ctx: TypeResolver
    ): List<ExpEmbedding> {
        if (!condition.match(function, ctx)) return listOf()
        return buildList {
            val receiver = function.formalArgs[0]
            val index = function.formalArgs[1]
            add(permission(receiver, ctx))
            add(boundCheck(receiver, index, ctx))
        }
    }

    override fun getPostconditions(
        returnVariable: VariableEmbedding, function: NamedFunctionSignature, ctx: TypeResolver
    ): List<ExpEmbedding> {
        if (!condition.match(function, ctx)) return listOf()
        val anon = PlaceholderVariableEmbedding(AnonymousBuiltinName(0), buildType { int() })
        val receiver = function.formalArgs[0]
        val index = function.formalArgs[1]
        val predicate = permission(receiver, ctx)
        val wrap = { inner: ExpEmbedding -> wrapUnfold(predicate, inner) }
        return buildList {
            add(permission(receiver, ctx))
        } + listOf(
            EqCmp(
                Size(wrap(IntArrayProperty.property.getter!!.getValue(receiver, ctx))),
                Size(Old(wrap(IntArrayProperty.property.getter.getValue(receiver, ctx))))
            ),
            ForAllEmbedding(
                anon,
                listOf(
                    Implies(
                        OperatorExpEmbeddings.And(
                            boundCheck(receiver, anon, ctx),
                            Not(
                                EqCmp(
                                    anon, index
                                )
                            )
                        ),
                        EqCmp(
                            SeqLookup(wrap(IntArrayProperty.property.getter!!.getValue(receiver, ctx)), anon),
                            SeqLookup(Old(wrap(IntArrayProperty.property.getter.getValue(receiver, ctx))), anon),
                        )

                    )

                )
            ),
            EqCmp(
                SeqLookup(
                    wrap(IntArrayProperty.property.getter!!.getValue(function.formalArgs[0], ctx)),
                    function.formalArgs[1]
                ), function.formalArgs[2]
            )
        )
    }

    override val condition = IntArrayAnyFunctionMatcher("set")
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
    val functionPrecon =
        listOf(IntArrayLengthConstructor, IntArrayGetFunction, IntArraySetFunction, IntArraySizeFunction).flatMap {
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

    val functionPostcon =
        listOf(IntArrayLengthConstructor, IntArrayGetFunction, IntArraySetFunction, IntArraySizeFunction).flatMap {
        it.getPostconditions(returnVariable, this, ctx)
    }


    return postcon + functionPostcon
}
