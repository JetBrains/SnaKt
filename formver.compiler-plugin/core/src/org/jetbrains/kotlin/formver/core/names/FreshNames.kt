/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.*

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

    override val candidates: List<CandidateName>
        get() = buildCandidates {
            candidate {
                +"anon"
            }
            candidate {
                +"anon"
                +"$n"
            }
        }
}

data class AnonymousBuiltinName(val n: Int) : FreshName {

    override val nameType: NameType
        get() = NameType.Variables

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = $$"anon$builtin"

    override val candidates: List<CandidateName>
        get() = buildCandidates {
            candidate {
                +"anon"
            }
            candidate {
                +"anon"
                +"builtin"
            }
            candidate {
                +"anon"
                +"builtin"
                +"$n"
            }
        }
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

    override val candidates: List<CandidateName>
        get() = buildCandidates {
            candidate {
                +"ret"
            }
        }
}

data class ReturnVariableName(val n: Int) : FreshName {
    override val nameType: NameType
        get() = NameType.Variables

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = $$"ret$$${n}"

    override val candidates: List<CandidateName>
        get() = buildCandidates {
            candidate {
                +"ret"
            }
            candidate {
                +"ret"
                +"$n"
            }
        }
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

    override val candidates: List<CandidateName>
        get() = buildCandidates {
            candidate {
                +"res"
            }
            candidate {
                +"result"
            }
        }
}

data object DispatchReceiverName : FreshName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = $$"this$dispatch"

    override val candidates: List<CandidateName>
        get() = buildCandidates {
            candidate {
                +"this"
            }
            candidate {
                +"this"
                +"dispatch"
            }
        }
}

data object ExtensionReceiverName : FreshName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = $$"this$extension"

    override val candidates: List<CandidateName>
        get() = buildCandidates {
            candidate {
                +"this"
            }
            candidate {
                +"this"
                +"extension"
            }
        }
}

data class SpecialName(val baseName: String) : FreshName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = baseName
    override val nameType: NameType
        get() = NameType.Special

    override val candidates: List<CandidateName>
        get() = buildCandidates {
            candidate {
                +baseName
            }
            candidate {
                +nameType
                +baseName
            }
        }
}

abstract class NumberedLabelName(override val nameType: NameType, open val n: Int) : FreshName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = n.toString()
    override val candidates: List<CandidateName>
        get() = buildCandidates {
            candidate {
                +nameType
            }
            candidate {
                +nameType
                +"$n"
            }
            candidate {
                +"lbl"
                +nameType
                +"$n"
            }
        }
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
    override val candidates: List<CandidateName>
        get() = buildCandidates {
            candidate {
                +"arg"
            }
            candidate {
                +"arg"
                +"$n"
            }
        }
}

data class DomainFuncParameterName(val baseName: String) : FreshName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = baseName
    override val candidates: List<CandidateName>
        get() = buildCandidates {
            candidate {
                +baseName
            }
        }
}

data class SsaVariableName(val ssaIndex: Int, val baseName: SymbolicName) : FreshName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "${baseName.mangled}$$ssaIndex"

    override val candidates: List<CandidateName>
        get() = buildCandidates {
            candidate {
                +baseName
                +"$ssaIndex"
            }
        }
}