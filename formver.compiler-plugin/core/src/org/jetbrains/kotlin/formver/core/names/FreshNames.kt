/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.NameType
import org.jetbrains.kotlin.formver.viper.NameType.Label
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.mangled

/* This file contains mangled names for constructs introduced during the conversion to Viper.
 *
 * See the NameEmbeddings file for guidelines on good name choices.
 */

/**
 * Representation for names not present in the original source,
 * e.g. storage for the result of subexpressions.
 */
data class AnonymousName(val n: Int) : SymbolicName {
    override val mangledType: NameType
        get() = NameType.Variable

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "anon$$n"
}

data class AnonymousBuiltinName(val n: Int) : SymbolicName {

    override val mangledType: NameType
        get() = NameType.Variable

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = $$"anon$builtin$$$n"
}

/**
 * Name for return variable that should *only* be used in signatures of methods without a body.
 */
data object PlaceholderReturnVariableName : SymbolicName {

    override val mangledType: NameType
        get() = NameType.Variable

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "ret"
}

data class ReturnVariableName(val n: Int) : SymbolicName {
    override val mangledType: NameType
        get() = NameType.Variable

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "ret$$n"
}

/**
 * Name for return variable that should *only* be used in signatures of pure functions
 * This variable will be translated into the special result variable in Viper
 */
data object FunctionResultVariableName : SymbolicName {
    override val mangledType: NameType
        get() = NameType.Variable

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "result"
}

data object DispatchReceiverName : SymbolicName {
    override val mangledType: NameType
        get() = NameType.Variable

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = $$"this$dispatch"
}

data object ExtensionReceiverName : SymbolicName {
    override val mangledType: NameType
        get() = NameType.Variable

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = $$"this$extension"
}

data class SpecialName(val baseName: String) : SymbolicName {
    override val mangledType: NameType
        get() = NameType.Special

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = baseName
}

abstract class NumberedLabelName(val baseName: String, val originalN: Int) : SymbolicName {
    override val mangledType = Label

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "$baseName$$originalN"
}

data class ReturnLabelName(val scopeDepth: Int) : NumberedLabelName("ret", scopeDepth)
data class BreakLabelName(val n: Int) : NumberedLabelName("break", n)
data class ContinueLabelName(val n: Int) : NumberedLabelName("cont", n)
data class CatchLabelName(val n: Int) : NumberedLabelName("catch", n)
data class TryExitLabelName(val n: Int) : NumberedLabelName("tryExit", n)


data class PlaceholderArgumentName(val n: Int) : SymbolicName {
    override val mangledType: NameType
        get() = NameType.Variable

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "arg$$n"
}

data class DomainFuncParameterName(val baseName: String) : SymbolicName {
    override val mangledType: NameType
        get() = NameType.Variable
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = baseName
}

data class SsaVariableName(val ssaIndex: Int, val baseName: SymbolicName) : SymbolicName {
    override val mangledType: NameType
        get() = NameType.Variable

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "${baseName.mangled}$$ssaIndex"
}