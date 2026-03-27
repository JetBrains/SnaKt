/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.NameType
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.mangled

/* This file contains mangled names for constructs introduced during the conversion to Viper.
 *
 * See the NameEmbeddings file for guidelines on good name choices.
 */

interface FreshName : SymbolicName


/**
 * Representation for names not present in the original source,
 * e.g. storage for the result of subexpressions.
 */
data class AnonymousName(val n: Int) : FreshName {
    override val nameType: NameType
        get() = NameType.Variables

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "anon$$${n}"
}

data class AnonymousBuiltinName(val n: Int) : FreshName {

    override val nameType: NameType
        get() = NameType.Variables

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = $$"anon$builtin"
}

/**
 * Name for return variable that should *only* be used in signatures of methods without a body.
 */
data object PlaceholderReturnVariableName : FreshName {

    override val nameType: NameType
        get() = NameType.Variables

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "ret"
}

data class ReturnVariableName(val n: Int) : FreshName {
    override val nameType: NameType
        get() = NameType.Variables

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = $$"ret$$${n}"
}

/**
 * Name for return variable that should *only* be used in signatures of pure functions
 * This variable will be translated into the special result variable in Viper
 */
data object FunctionResultVariableName : FreshName {
    override val nameType: NameType
        get() = NameType.Variables

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "result"
}

data object DispatchReceiverName : FreshName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = $$"this$dispatch"
}

data object ExtensionReceiverName : FreshName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = $$"this$extension"
}

data class SpecialName(val baseName: String) : FreshName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = baseName
    override val nameType: NameType
        get() = NameType.Special
}

abstract class NumberedLabelName(override val nameType: NameType, open val n: Int) : FreshName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = n.toString()
}


data class ReturnLabelName(override val n: Int) : NumberedLabelName(NameType.Label.Return, n)
data class BreakLabelName(override val n: Int) : NumberedLabelName(NameType.Label.Break, n)
data class ContinueLabelName(override val n: Int) : NumberedLabelName(NameType.Label.Continue, n)
data class CatchLabelName(override val n: Int) : NumberedLabelName(NameType.Label.Catch, n)
data class TryExitLabelName(override val n: Int) : NumberedLabelName(NameType.Label.TryExit, n)


data class PlaceholderArgumentName(val n: Int) : FreshName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "arg$n"
}

data class DomainFuncParameterName(val baseName: String) : FreshName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = baseName
}

data class SsaVariableName(val ssaIndex: Int, val baseName: SymbolicName) : FreshName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "${baseName.mangled}$$ssaIndex"
}