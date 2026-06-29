/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.callables

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.conversion.insertForAllFunctionCall
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.AddCharInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.AddIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.And
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.DivIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.Implies
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.MulIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.NegInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.Not
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.Or
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.RemIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.StringGet
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.SubCharChar
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.SubCharInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.SubIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.Xor
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildFunctionPretype
import org.jetbrains.kotlin.formver.core.embeddings.types.nullableAny
import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


val SpecialKotlinFunction.callableId: CallableId
    get() = CallableId(FqName.fromSegments(packageName), className?.let { FqName(it) }, Name.identifier(name))

fun SpecialKotlinFunction.embedName(): ScopedName = callableId.embedFunctionName(callableType)

/**
 * We store here all the __Kotlin__ functions that need a (fully) special `ExpEmbedding`.
 * `byName` is stateless - it always stores the same Kotlin functions
 * and corresponding embeddings.
 */
object SpecialKotlinFunctions {
    private val contractBuilderTypeName = buildName {
        packageScope(SpecialPackages.contracts)
        ClassKotlinName(listOf("ContractBuilder"))
    }
    private val booleanArrayTypeName = buildName {
        packageScope(SpecialPackages.kotlin)
        ClassKotlinName(listOf("BooleanArray"))
    }
    private val invariantBuilderTypeName = buildName {
        packageScope(SpecialPackages.formver)
        ClassKotlinName(listOf("InvariantBuilder"))
    }
    private val uniquePredTypeName = buildName {
        packageScope(SpecialPackages.formver)
        ClassKotlinName(listOf("UniquePred"))
    }
    private val predicateTypeName = buildName {
        packageScope(SpecialPackages.formver)
        ClassKotlinName(listOf("Predicate"))
    }

    private val permissionTypeName = buildName {
        packageScope(SpecialPackages.formver)
        ClassKotlinName(listOf("Permission"))
    }

    val byName: Map<SymbolicName, FullySpecialKotlinFunction> = buildFullySpecialFunctions {
        val intIntToIntType = buildFunctionPretype {
            withDispatchReceiver { int() }
            withParam { int() }
            withReturnType { int() }
        }
        withCallableType(intIntToIntType) {
            addFunction(SpecialPackages.kotlin, className = "Int", name = "plus") { args, _ ->
                AddIntInt(args[0], args[1])
            }
            addFunction(SpecialPackages.kotlin, className = "Int", name = "minus") { args, _ ->
                SubIntInt(args[0], args[1])
            }
            addFunction(SpecialPackages.kotlin, className = "Int", name = "times") { args, _ ->
                MulIntInt(args[0], args[1])
            }
            addFunction(SpecialPackages.kotlin, className = "Int", name = "div") { args, _ ->
                DivIntInt(args[0], args[1])
            }
            addFunction(SpecialPackages.kotlin, className = "Int", name = "rem") { args, _ ->
                RemIntInt(args[0], args[1])
            }
            addFunction(SpecialPackages.kotlin, className = "Int", name = "compareTo") { args, _ ->
                SubIntInt(args[0], args[1])
            }
        }

        val intToIntType = buildFunctionPretype {
            withDispatchReceiver { int() }
            withReturnType { int() }
        }

        withCallableType(intToIntType) {
            addFunction(SpecialPackages.kotlin, className = "Int", name = "inc") { args, _ ->
                AddIntInt(args[0], IntLit(1))
            }
            addFunction(SpecialPackages.kotlin, className = "Int", name = "dec") { args, _ ->
                SubIntInt(args[0], IntLit(1))
            }
            addFunction(SpecialPackages.kotlin, className = "Int", name = "unaryMinus") { args, _ ->
                NegInt(args[0])
            }
            addFunction(SpecialPackages.kotlin, className = "Int", name = "unaryPlus") { args, _ ->
                args[0]
            }
        }

        val booleanToBooleanType = buildFunctionPretype {
            withDispatchReceiver { boolean() }
            withReturnType { boolean() }
        }
        val booleanBooleanToBooleanType = buildFunctionPretype {
            withDispatchReceiver { boolean() }
            withParam { boolean() }
            withReturnType { boolean() }
        }

        withCallableType(booleanBooleanToBooleanType) {
            addFunction(
                booleanBooleanToBooleanType, SpecialPackages.kotlin, className = "Boolean", name = "and"
            ) { args, _ ->
                And(args[0], args[1])
            }

            addFunction(
                booleanBooleanToBooleanType, SpecialPackages.kotlin, className = "Boolean", name = "or"
            ) { args, _ ->
                Or(args[0], args[1])
            }

            addFunction(
                booleanBooleanToBooleanType, SpecialPackages.kotlin, className = "Boolean", name = "xor"
            ) { args, _ ->
                Xor(args[0], args[1])
            }

        }
        addFunction(booleanToBooleanType, SpecialPackages.kotlin, className = "Boolean", name = "not") { args, _ ->
            Not(args[0])
        }

        val extBooleanBooleanToBooleanType = buildFunctionPretype {
            withExtensionReceiver { boolean() }
            withParam { boolean() }
            withReturnType { boolean() }
        }

        addFunction(extBooleanBooleanToBooleanType, SpecialPackages.formver, name = "implies") { args, _ ->
            Implies(args[0], args[1])
        }

        val oldCallableType = buildFunctionPretype {
            withParam { nullableAny() }
            withReturnType { nullableAny() }
        }
        addFunction(oldCallableType, SpecialPackages.formver, name = "old") { args, _ ->
            Old(args[0])
        }

        val verifyCallableType = buildFunctionPretype {
            withParam {
                klass {
                    withName(booleanArrayTypeName)
                }
            }
            withReturnType { unit() }
        }
        addFunction(verifyCallableType, SpecialPackages.formver, name = "verify") { args, _ ->
            args.map { Assert(it) }.toBlock()
        }

        val forAllCallableType = buildFunctionPretype {
            withParam {
                function {
                    withDispatchReceiver {
                        klass {
                            withName(invariantBuilderTypeName)
                        }
                    }
                    withParam {
                        nullableAny()
                    }
                    withReturnType {
                        unit()
                    }
                }
            }
            withReturnType {
                boolean()
            }
        }

        addFunction(forAllCallableType, SpecialPackages.formver, name = "forAll") { args, ctx ->
            val arg = args.first()
            val lambda = arg.ignoringMetaNodes() as? LambdaExp ?: throw SnaktInternalException(
                null, "First argument of forAll function must be a lambda."
            )
            val param = lambda.function.valueParameters.first()
            val body = lambda.function.body ?: throw SnaktInternalException(
                null, "Lambda body of forAll function must be present."
            )
            ctx.insertForAllFunctionCall(param.symbol, body)
        }

        val permissionCallableType = buildFunctionPretype {
            withReturnType {
                klass {
                    withName(permissionTypeName)
                }
            }
        }
        withCallableType(permissionCallableType) {
            addFunction(SpecialPackages.formver, name = "read") { _, _ -> PermissionLit(PermExp.WildcardPerm()) }
            addFunction(SpecialPackages.formver, name = "write") { _, _ -> PermissionLit(PermExp.FullPerm()) }
        }

        fun extractPermission(exp: ExpEmbedding?): PermissionLit {
            return when (val perm = exp?.ignoringCastsAndMetaNodes()) {
                null -> PermissionLit(PermExp.FullPerm())
                is PermissionLit -> perm
                else -> throw SnaktInternalException(
                    null, "Second argument of `acc` must be `read()` or `write()`."
                )
            }
        }

        fun extractPredicate(exp: MethodCall, permissions: ExpEmbedding?): PredicateAccessPermissions {
            val permissions = extractPermission(permissions)
            return when (exp.type.pretype.name) {
                uniquePredTypeName -> {
                    val arg = exp.args.firstOrNull()!!
                    val type = arg.type.pretype as? ClassTypeEmbedding ?: throw SnaktInternalException(
                        null,
                        "Can only unfold the unique predicates of classes"
                    )
                    PredicateAccessPermissions(
                        type.uniquePredicateName,
                        listOf(arg.ignoringCastsAndMetaNodes()),
                        permissions.perm
                    )
                }

                else -> throw SnaktInternalException(null, "Unknown predicate")
            }
        }

        val accCallableType = buildFunctionPretype {
            withParam { nullableAny() }
            withParam {
                isNullable = true
                klass {
                    withName(permissionTypeName)
                }
            }
            withReturnType { boolean() }
        }
        addFunction(accCallableType, SpecialPackages.formver, name = "acc") { args, ctx ->
            val source = (args.firstOrNull() as? WithPosition)?.source
            val perm = extractPermission(args.getOrNull(1))
            when (val exp = args.firstOrNull()?.ignoringCastsAndMetaNodes()) {
                null -> throw SnaktInternalException(
                    source, "First argument of `acc` must be a field access like `x.a`."
                )
                is FieldAccess -> AccEmbedding(exp.receiver, exp.field, perm.perm)
                is MethodCall -> extractPredicate(exp, args.getOrNull(1))
                else -> throw SnaktInternalException(
                    source, "First argument of `acc` must be a field access like `x.a` or a predicate."
                )
            }


        }

        val invariantsBuilderCallableType = buildFunctionPretype {
            withParam {
                function {
                    withReturnType { unit() }
                }
            }
            withReturnType { unit() }
        }
        withCallableType(invariantsBuilderCallableType) {
            addNoOpFunction(SpecialPackages.formver, name = "loopInvariants")
            addNoOpFunction(SpecialPackages.formver, name = "preconditions")
        }

        val postconditionsBuilderCallableType = buildFunctionPretype {
            withParam {
                function {
                    withParam { nullableAny() }
                    withReturnType { unit() }
                }
            }
            withReturnType { unit() }
        }

        withCallableType(postconditionsBuilderCallableType) {
            addNoOpFunction(SpecialPackages.formver, name = "postconditions")
        }

        val contractCallableType = buildFunctionPretype {
            withParam {
                function {
                    withDispatchReceiver {
                        klass {
                            withName(contractBuilderTypeName)
                        }
                    }
                    withReturnType { unit() }
                }
            }
            withReturnType { unit() }
        }

        addFunction(contractCallableType, SpecialPackages.contracts, name = "contract") { _, _ ->
            UnitLit
        }

        val charCharToIntType = buildFunctionPretype {
            withDispatchReceiver { char() }
            withParam { char() }
            withReturnType { int() }
        }

        addFunction(charCharToIntType, SpecialPackages.kotlin, className = "Char", name = "minus") { args, _ ->
            SubCharChar(args[0], args[1])
        }

        val charIntToCharType = buildFunctionPretype {
            withDispatchReceiver { char() }
            withParam { int() }
            withReturnType { char() }
        }

        withCallableType(charIntToCharType) {
            addFunction(SpecialPackages.kotlin, className = "Char", name = "plus") { args, _ ->
                AddCharInt(args[0], args[1])
            }
            addFunction(SpecialPackages.kotlin, className = "Char", name = "minus") { args, _ ->
                SubCharInt(args[0], args[1])
            }
        }

        val stringIntToCharType = buildFunctionPretype {
            withDispatchReceiver { string() }
            withParam {

                int()
            }
            withReturnType { char() }
        }

        addFunction(stringIntToCharType, SpecialPackages.kotlin, className = "String", name = "get") { args, _ ->
            StringGet(args[0], args[1])
        }

        val intArrayGetType = buildFunctionPretype {
            withDispatchReceiver { intArray() }
            withParam { int() }
            withReturnType { int() }
        }
        addFunction(intArrayGetType, SpecialPackages.kotlin, className = "IntArray", name = "get") { args, _ ->
            IntArrayGet(args[0], args[1])
        }

        val intArraySetType = buildFunctionPretype {
            withDispatchReceiver { intArray() }
            withParam { int() }
            withParam { int() }
            withReturnType { unit() }
        }
        addFunction(intArraySetType, SpecialPackages.kotlin, className = "IntArray", name = "set") { args, _ ->
            IntArraySet(args[0], args[1], args[2])
        }

        val uniquePredicatePermissionsToUnit = buildFunctionPretype {
            withParam {
                klass {
                    withName(predicateTypeName)
                }
            }
            withParam {
                isNullable = true
                klass {
                    withName(permissionTypeName)
                }
            }
            withReturnType { unit() }
        }
        addFunction(uniquePredicatePermissionsToUnit, SpecialPackages.formver, name = "unfold") { args, _ ->
            val exp = (args[0].ignoringMetaNodes() as? MethodCall) ?: throw SnaktInternalException(
                null, "First argument of unfold must be constructor to a predicate."
            )
            Unfold(
                extractPredicate(exp, args.getOrNull(1))
            )
        }

        addFunction(uniquePredicatePermissionsToUnit, SpecialPackages.formver, name = "fold") { args, _ ->
            val exp = (args[0].ignoringMetaNodes() as? MethodCall) ?: throw SnaktInternalException(
                null, "First argument of unfold must be constructor to a predicate."
            )
            Fold(
                extractPredicate(exp, args.getOrNull(1))
            )
        }


        val intArrayToAny = buildFunctionPretype {
            withParam { intArray() }
            withReturnType { any() }
        }
        addFunction(intArrayToAny, SpecialPackages.formver, name = "toMultiset") { args, _ ->
            IntArrayAsMultiset(args[0])
        }
    }
}

val CallableEmbedding.isVerifyFunction: Boolean
    get() = isFormverPluginFunctionNamed(name = "verify")

fun CallableEmbedding.isFormverPluginFunctionNamed(className: String? = null, name: String): Boolean =
    this is FullySpecialKotlinFunction && NameMatcher.matchClassScope(this.embedName()) {
        ifPackageName(SpecialPackages.formver) {
            if (className == null) {
                ifNoReceiver {
                    ifFunctionName(name) {
                        return true
                    }
                }
            } else {
                ifClassName(className) {
                    ifFunctionName(name) {
                        return true
                    }
                }
            }
        }
        return false
    }
