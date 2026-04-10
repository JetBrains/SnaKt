/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.viper.NameType
import org.jetbrains.kotlin.formver.viper.SymbolicName

/* This file contains mangled names for constructs introduced during the conversion to Viper.
 *
 * See the NameEmbeddings file for guidelines on good name choices.
 */


sealed interface FreshName : SymbolicName

abstract class TypedFreshName(override val mangledType: NameType, val baseName: String) : FreshName

/**
 * Representation for names not present in the original source,
 * e.g. storage for the result of subexpressions.
 */
data class AnonymousName(val n: Int) : TypedFreshName(NameType.Base.Variable, "anon$$n")

data class AnonymousBuiltinName(val n: Int) : TypedFreshName(NameType.Base.Variable, $$"anon$builtin$$$n")

/**
 * Name for return variable that should *only* be used in signatures of methods without a body.
 */
data object PlaceholderReturnVariableName : TypedFreshName(NameType.Base.Variable, "ret")

data class ReturnVariableName(val n: Int) : TypedFreshName(NameType.Base.Variable, "ret$$n")

/**
 * Name for return variable that should *only* be used in signatures of pure functions
 * This variable will be translated into the special result variable in Viper
 */
data object FunctionResultVariableName : TypedFreshName(NameType.Base.Variable, "result")

data object DispatchReceiverName : TypedFreshName(NameType.Base.Variable, $$"this$dispatch")

data object ExtensionReceiverName : TypedFreshName(NameType.Base.Variable, $$"this$extension")

data class SpecialFieldName(val name: String) : TypedFreshName(NameType.Member.Property, name)


sealed class NumberedLabelName(name: String, val originalN: Int) : TypedFreshName(NameType.Base.Label, name) {}

data class ReturnLabelName(val scopeDepth: Int) : NumberedLabelName("ret", scopeDepth)
data class BreakLabelName(val n: Int) : NumberedLabelName("break", n)
data class ContinueLabelName(val n: Int) : NumberedLabelName("cont", n)
data class CatchLabelName(val n: Int) : NumberedLabelName("catch", n)
data class TryExitLabelName(val n: Int) : NumberedLabelName("tryExit", n)


data class DomainAssociatedFuncName(val name: String) : TypedFreshName(NameType.Base.DomainFunction, name)

data class PlaceholderArgumentName(val n: Int) : TypedFreshName(NameType.Base.Variable, "arg$$n")

data class DomainFuncParameterName(val name: String) : TypedFreshName(NameType.Base.Variable, name)

data class SsaVariableName(val ssaIndex: Int, val baseName: SymbolicName) : FreshName {
    override val mangledType: NameType
        get() = NameType.Base.Variable
}

data class PredicateName(val name: String) : TypedFreshName(NameType.Base.Predicate, name)

data class HavocName(val type: TypeEmbedding) : SymbolicName {
    override val mangledType: NameType
        get() = NameType.Base.Havoc
}