/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.NameType
import org.jetbrains.kotlin.formver.viper.NameType.Label
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.mangled

/* This file contains mangled names for constructs introduced during the conversion to Viper.
 *
 * See the NameEmbeddings file for guidelines on good name choices.
 */


sealed interface FreshName : SymbolicName

abstract class TypedFreshName(override val mangledType: NameType, val baseName: String) : FreshName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = baseName
}

/**
 * Representation for names not present in the original source,
 * e.g. storage for the result of subexpressions.
 */
data class AnonymousName(val n: Int) : TypedFreshName(NameType.Variable, "anon$$n")

data class AnonymousBuiltinName(val n: Int) : TypedFreshName(NameType.Variable, $$"anon$builtin$$$n")

/**
 * Name for return variable that should *only* be used in signatures of methods without a body.
 */
data object PlaceholderReturnVariableName : TypedFreshName(NameType.Variable, "ret")

data class ReturnVariableName(val n: Int) : TypedFreshName(NameType.Variable, "ret$$n")

/**
 * Name for return variable that should *only* be used in signatures of pure functions
 * This variable will be translated into the special result variable in Viper
 */
data object FunctionResultVariableName : TypedFreshName(NameType.Variable, "result")

data object DispatchReceiverName : TypedFreshName(NameType.Variable, $$"this$dispatch")

data object ExtensionReceiverName : TypedFreshName(NameType.Variable, $$"this$extension")

data class SpecialFieldName(val name: String) : TypedFreshName(NameType.Property, name)


abstract class NumberedLabelName(val baseName: String, val originalN: Int) : TypedFreshName(Label) {

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "$baseName$$originalN"
}

data class ReturnLabelName(val scopeDepth: Int) : NumberedLabelName("ret", scopeDepth)
data class BreakLabelName(val n: Int) : NumberedLabelName("break", n)
data class ContinueLabelName(val n: Int) : NumberedLabelName("cont", n)
data class CatchLabelName(val n: Int) : NumberedLabelName("catch", n)
data class TryExitLabelName(val n: Int) : NumberedLabelName("tryExit", n)


data class DomainAssociatedFuncName(val name: String) : TypedFreshName(NameType.DomainFunction, name)

data class PlaceholderArgumentName(val n: Int) : TypedFreshName(NameType.Variable, "arg$$n")

data class DomainFuncParameterName(val name: String) : TypedFreshName(NameType.Variable, name)

data class SsaVariableName(val ssaIndex: Int, val baseName: SymbolicName) : FreshName {
    override val mangledType: NameType
        get() = NameType.Variable

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "${baseName.mangled}$$ssaIndex"
}

data class PredicateName(val name: String) : TypedFreshName(NameType.Predicate, name)

data class HavocName(val type: TypeEmbedding) : SymbolicName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = type.name.mangled
    override val mangledType: NameType
        get() = NameType.Havoc
}