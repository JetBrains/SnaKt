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


sealed interface FreshName : SymbolicName {
    override val nameType: NameType
}

sealed interface NumberedName : FreshName {
    val n: Int
}

/**
 * Representation for names not present in the original source,
 * e.g. storage for the result of subexpressions.
 */
// "anon$n"
data class AnonymousName(override val n: Int) : NumberedName {
    override val nameType: NameType = NameType.Base.Variable
}

// $$"anon$builtin$$$n"
data class AnonymousBuiltinName(override val n: Int) : NumberedName {
    override val nameType: NameType = NameType.Base.Variable
}

/**
 * Name for return variable that should *only* be used in signatures of methods without a body.
 */
// ret
data object PlaceholderReturnVariableName : FreshName {
    override val nameType: NameType = NameType.Base.Variable

}

//ret$n
data class ReturnVariableName(override val n: Int) : NumberedName {
    override val nameType: NameType = NameType.Base.Variable
}

/**
 * Name for return variable that should *only* be used in signatures of pure functions
 * This variable will be translated into the special result variable in Viper
 */
// , "result"
data object FunctionResultVariableName : FreshName {
    override val nameType: NameType = NameType.Base.Variable
}

// "this$dispatch"
data object DispatchReceiverName : FreshName {
    override val nameType: NameType = NameType.Base.Variable
}

// "this$extension"
data object ExtensionReceiverName : FreshName {
    override val nameType: NameType = NameType.Base.Variable
}

data class SpecialFieldName(val name: String) : FreshName {
    override val nameType: NameType = NameType.Member.Property
}

sealed class LabelName(override val n: Int) : NumberedName {
    override val nameType: NameType = NameType.Base.Label
}

data class ReturnLabelName(override val n: Int) : LabelName(n)
data class BreakLabelName(override val n: Int) : LabelName(n)
data class ContinueLabelName(override val n: Int) : LabelName(n)
data class CatchLabelName(override val n: Int) : LabelName(n)
data class TryExitLabelName(override val n: Int) : LabelName(n)


data class DomainAssociatedFuncName(val name: String) : FreshName {
    override val nameType: NameType = NameType.Base.DomainFunction
}

// "arg$$n"
data class PlaceholderArgumentName(override val n: Int) : NumberedName {
    override val nameType = NameType.Base.Variable
}

data class DomainFuncParameterName(val name: String) : FreshName {
    override val nameType = NameType.Base.Variable
}

data class SsaVariableName(override val n: Int, val baseName: SymbolicName) : NumberedName {
    override val nameType: NameType = NameType.Base.Variable
}

data class PredicateName(val name: String) : FreshName {
    override val nameType: NameType = NameType.Base.Predicate
}

data class HavocName(val type: TypeEmbedding) : FreshName {
    override val nameType: NameType = NameType.Base.Havoc
}