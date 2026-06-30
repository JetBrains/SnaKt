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
import org.jetbrains.kotlin.formver.core.embeddings.types.PretypeEmbedding
import org.jetbrains.kotlin.formver.core.names.NameMatcher
import org.jetbrains.kotlin.formver.core.names.SpecialPackages
import org.jetbrains.kotlin.formver.viper.SymbolicName

private fun VariableEmbedding.sameSize(): ExpEmbedding =
    EqCmp(FieldAccess(this, CollectionSizeFieldEmbedding), Old(FieldAccess(this, CollectionSizeFieldEmbedding)))

private fun VariableEmbedding.increasedSize(amount: Int): ExpEmbedding = EqCmp(
    FieldAccess(this, CollectionSizeFieldEmbedding),
    OperatorExpEmbeddings.AddIntInt(Old(FieldAccess(this, CollectionSizeFieldEmbedding)), IntLit(amount)),
)

/**
 * Matches a stdlib function against a set of AND-combined criteria.
 *
 * [pkg] + [receiverSubtypeName]: the dispatch receiver must be a subtype of `pkg.receiverInherits`.
 * [pkg] is not re-checked against the function's own package in this case — overrides in
 * user-defined classes match just as well as the original declarations.
 *
 * [pkg] + [noReceiver] / [functionName] / [constructorOf]: the function itself must live in [pkg].
 * Use this for top-level functions (e.g. `emptyList`) and constructors.
 *
 * [receiverSubtypeName] and [noReceiver] are mutually exclusive.
 * [functionName] and [constructorOf] are mutually exclusive.
 */
class StdLibFunctionSpec(
    val pkg: List<String>? = null,
    val receiverSubtypeName: String? = null,
    val noReceiver: Boolean = false,
    val functionName: String? = null,
    val constructorOf: String? = null,
) {
    init {
        require(!(receiverSubtypeName != null && noReceiver)) { "receiverInherits and noReceiver are mutually exclusive" }
        require(receiverSubtypeName == null || pkg != null) { "pkg must be set when receiverInherits is set" }
        require(constructorOf == null || functionName == null) { "constructorOf and functionName are mutually exclusive" }
    }

    fun matches(function: NamedFunctionSignature, typeResolver: TypeResolver): Boolean {
        if (!checkReceiverSubtype(function, typeResolver)) return false
        if (!needsNameCheck()) return true

        return matchesNameScope(function.name)
    }

    private fun checkReceiverSubtype(
        function: NamedFunctionSignature,
        typeResolver: TypeResolver
    ): Boolean {
        if (receiverSubtypeName == null) return true // Pass if there's no inheritance constraint

        val receiverPretype = function.callableType.dispatchReceiverType?.pretype ?: return false
        return typeResolver.isInheritorOf(receiverPretype, pkg!!, receiverSubtypeName)
    }

    private fun needsNameCheck(): Boolean {
        return functionName != null ||
                constructorOf != null ||
                noReceiver ||
                (pkg != null && receiverSubtypeName == null)
    }

    private fun matchesNameScope(name: SymbolicName): Boolean {
        NameMatcher.matchClassScope(name) {
            if (pkg != null && receiverSubtypeName == null) {
                var pkgMatched = false
                ifPackageName(pkg) { pkgMatched = true }
                if (!pkgMatched) return false
            }

            when {
                noReceiver && functionName != null -> ifNoReceiver { ifFunctionName(functionName) { return true } }
                noReceiver && constructorOf != null -> ifNoReceiver { ifConstructorOf(constructorOf) { return true } }
                noReceiver -> ifNoReceiver { return true }
                functionName != null -> ifFunctionName(functionName) { return true }
                constructorOf != null -> ifConstructorOf(constructorOf) { return true }
                else -> return true
            }
            return false
        }
    }
}

/**
 * Matches any parameter whose pretype is a subtype of `[pkg].[paramTypeInherits]`.
 * If the parameter type is nullable, the generated embeddings are wrapped as
 * `param != null implies condition`.
 */
data class StdLibParamSpec(
    val pkg: List<String>,
    val paramTypeInherits: String,
) {
    fun matches(paramPretype: PretypeEmbedding, typeResolver: TypeResolver): Boolean =
        typeResolver.isInheritorOf(paramPretype, pkg, paramTypeInherits)
}

sealed interface StdLibCondition {
    val spec: StdLibFunctionSpec
    fun matches(function: NamedFunctionSignature, typeResolver: TypeResolver): Boolean =
        spec.matches(function, typeResolver)
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

sealed interface StdLibParamPrecondition {
    val spec: StdLibParamSpec
    fun matches(paramPretype: PretypeEmbedding, typeResolver: TypeResolver): Boolean =
        spec.matches(paramPretype, typeResolver)

    fun getEmbeddings(param: VariableEmbedding): List<ExpEmbedding>

    companion object {
        val all: List<StdLibParamPrecondition> = listOf()
    }
}

sealed interface StdLibParamPostcondition {
    val spec: StdLibParamSpec
    fun matches(paramPretype: PretypeEmbedding, typeResolver: TypeResolver): Boolean =
        spec.matches(paramPretype, typeResolver)

    fun getEmbeddings(returnVariable: VariableEmbedding, param: VariableEmbedding): List<ExpEmbedding>

    companion object {
        val all: List<StdLibParamPostcondition> = listOf()
    }
}

data object GetPrecondition : StdLibPrecondition {
    override val spec = StdLibFunctionSpec(
        pkg = SpecialPackages.collections,
        receiverSubtypeName = "List",
        functionName = "get",
    )

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
    override val spec = StdLibFunctionSpec(
        pkg = SpecialPackages.collections,
        receiverSubtypeName = "List",
        functionName = "subList",
    )

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
    override val spec = StdLibFunctionSpec(
        pkg = SpecialPackages.collections,
        noReceiver = true,
        functionName = "emptyList",
    )

    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> = listOf(EqCmp(FieldAccess(returnVariable, CollectionSizeFieldEmbedding), IntLit(0)))
}

data object IsEmptyPostcondition : StdLibPostcondition {
    override val spec = StdLibFunctionSpec(
        pkg = SpecialPackages.collections,
        receiverSubtypeName = "Collection",
        functionName = "isEmpty",
    )

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
    override val spec = StdLibFunctionSpec(
        pkg = SpecialPackages.collections,
        receiverSubtypeName = "List",
        functionName = "get",
    )

    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> = listOf(function.dispatchReceiver!!.sameSize())
}

data object SubListPostcondition : StdLibPostcondition {
    override val spec = StdLibFunctionSpec(
        pkg = SpecialPackages.collections,
        receiverSubtypeName = "List",
        functionName = "subList",
    )

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
    override val spec = StdLibFunctionSpec(
        pkg = SpecialPackages.collections,
        receiverSubtypeName = "MutableList",
        functionName = "add",
    )

    override fun getEmbeddings(
        returnVariable: VariableEmbedding,
        function: NamedFunctionSignature
    ): List<ExpEmbedding> = listOf(function.dispatchReceiver!!.increasedSize(1))
}

fun NamedFunctionSignature.stdLibPreconditions(ctx: TypeResolver): List<ExpEmbedding> {
    val fromFunction = StdLibPrecondition.all.filter { it.matches(this, ctx) }.flatMap { it.getEmbeddings(this) }
    val fromParams = params.flatMap { param ->
        StdLibParamPrecondition.all.filter { it.matches(param.type.pretype, ctx) }.flatMap { it.getEmbeddings(param) }
            .map { if (param.type.isNullable) Implies(param.notNullCmp(), it) else it }
    }
    return fromFunction + fromParams
}

fun NamedFunctionSignature.stdLibPostconditions(
    returnVariable: VariableEmbedding,
    ctx: TypeResolver,
): List<ExpEmbedding> {
    val fromFunction =
        StdLibPostcondition.all.filter { it.matches(this, ctx) }.flatMap { it.getEmbeddings(returnVariable, this) }
    val fromParams = params.flatMap { param ->
        StdLibParamPostcondition.all.filter { it.matches(param.type.pretype, ctx) }
            .flatMap { it.getEmbeddings(returnVariable, param) }
            .map { if (param.type.isNullable) Implies(param.notNullCmp(), it) else it }
    }
    return fromFunction + fromParams
}
