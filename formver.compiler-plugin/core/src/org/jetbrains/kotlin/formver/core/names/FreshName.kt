/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.names.shortNameResolver.buildCandidates
import org.jetbrains.kotlin.formver.core.names.shortNameResolver.nameOnlyCandidates
import org.jetbrains.kotlin.formver.core.names.shortNameResolver.nameWithPrefixAndSuffixCandidates
import org.jetbrains.kotlin.formver.core.names.shortNameResolver.nameWithPrefixCandidates
import org.jetbrains.kotlin.formver.viper.AnyName
import org.jetbrains.kotlin.formver.viper.CandidateName
import org.jetbrains.kotlin.formver.viper.SymbolicName

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
 * Marker interface for names which describe a variable.
 */
sealed interface NameTypeIsVariable : FreshName {
    override val nameType
        get() = NameType.Base.Variable
}

/**
 * Representation for names not present in the original source,
 * e.g. storage for the result of subexpressions.
 */
data class AnonymousName(override val n: Int) : NumberedName, NameTypeIsVariable {
    override val inViper: Boolean = true

    override val candidates: List<CandidateName> = nameWithPrefixAndSuffixCandidates("anon", nameType, n.toString())

    override val children: List<AnyName> = listOf(nameType)
}

data class AnonymousBuiltinName(override val n: Int) : NumberedName, NameTypeIsVariable {
    override val inViper: Boolean = true

    override val candidates: List<CandidateName> = buildCandidates {
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
            +n.toString()
        }
    }

    override val children: List<AnyName> = listOf(nameType)
}

/**
 * Name for return variable that should *only* be used in signatures of methods without a body.
 */
data object PlaceholderReturnVariableName : FreshName {
    override val nameType: NameType = NameType.Base.Variable
    override val inViper: Boolean = true

    override val candidates: List<CandidateName> = nameWithPrefixCandidates("ret", nameType)

    override val children: List<AnyName> = listOf(nameType)
}

data class ReturnVariableName(override val n: Int) : NumberedName, NameTypeIsVariable {
    override val inViper: Boolean = true

    override val candidates: List<CandidateName> = nameWithPrefixAndSuffixCandidates("ret", nameType, n.toString())

    override val children: List<AnyName> = listOf(nameType)
}

/**
 * Name for return variable that should *only* be used in signatures of pure functions
 * This variable will be translated into the special result variable in Viper
 */
data object FunctionResultVariableName : FreshName, NameTypeIsVariable {
    override val inViper: Boolean = true

    override val candidates: List<CandidateName> = nameWithPrefixCandidates("result", nameType)

    override val children: List<AnyName> = listOf(nameType)
}

data object DispatchReceiverName : FreshName, NameTypeIsVariable {
    override val inViper: Boolean = true

    override val candidates: List<CandidateName> = nameWithPrefixCandidates("this", nameType)

    override val children: List<AnyName> = listOf(nameType)
}

data object ExtensionReceiverName : FreshName, NameTypeIsVariable {
    override val inViper: Boolean = true

    override val candidates: List<CandidateName> = nameWithPrefixCandidates("this", nameType)

    override val children: List<AnyName> = listOf(nameType)
}

data class SpecialFieldName(val name: String) : FreshName {
    override val nameType: NameType = NameType.Member.Property
    override val inViper: Boolean = true

    override val candidates: List<CandidateName> = nameWithPrefixCandidates(name, nameType)

    override val children: List<AnyName> = listOf(nameType)
}

sealed class LabelName(override val n: Int) : NumberedName {
    override val nameType: NameType = NameType.Base.Label
    override val inViper: Boolean = true

    override val candidates: List<CandidateName>
        get() {
            val name = when (this) {
                is BreakLabelName -> "break"
                is CatchLabelName -> "catch"
                is ContinueLabelName -> "cont"
                is ReturnLabelName -> "ret"
                is TryExitLabelName -> "tryExit"
            }
            return nameWithPrefixAndSuffixCandidates(name, nameType, n.toString())
        }

    override val children: List<AnyName>
        get() = listOf(nameType)
}

data class ReturnLabelName(override val n: Int) : LabelName(n)
data class BreakLabelName(override val n: Int) : LabelName(n)
data class ContinueLabelName(override val n: Int) : LabelName(n)
data class CatchLabelName(override val n: Int) : LabelName(n)
data class TryExitLabelName(override val n: Int) : LabelName(n)


data class DomainAssociatedFuncName(val name: String) : FreshName {
    override val nameType: NameType = NameType.Base.DomainFunction
    override val inViper: Boolean = true

    override val candidates: List<CandidateName> = nameWithPrefixCandidates(name, nameType)

    override val children: List<AnyName> = listOf(nameType)
}

data class PlaceholderArgumentName(override val n: Int) : NumberedName, NameTypeIsVariable {
    override val inViper: Boolean = true

    override val candidates: List<CandidateName> = nameWithPrefixAndSuffixCandidates("arg", nameType, n.toString())

    override val children: List<AnyName> = listOf(nameType)
}

data class DomainFuncParameterName(val name: String) : FreshName, NameTypeIsVariable {
    override val inViper: Boolean = true

    override val candidates: List<CandidateName> = nameWithPrefixCandidates(name, nameType)

    override val children: List<AnyName> = listOf(nameType)
}

data class SsaVariableName(override val n: Int, val baseName: SymbolicName) : NumberedName, NameTypeIsVariable {
    override val inViper: Boolean = true

    override val candidates: List<CandidateName> = buildCandidates {
        candidate {
            +baseName
        }
        candidate {
            +baseName
            +n.toString()
        }
        candidate {
            +nameType
            +baseName
            +n.toString()
        }
    }

    override val children: List<AnyName> = listOf(nameType, baseName)
}

data class PredicateName(val name: String) : FreshName {
    override val inViper: Boolean = false
    override val nameType: NameType = NameType.Base.Predicate

    override val candidates: List<CandidateName> = nameOnlyCandidates(name)

    override val children: List<AnyName> = listOf(nameType)
}

data class HavocName(val type: TypeEmbedding) : FreshName {
    override val inViper: Boolean = true
    override val nameType: NameType = NameType.Base.Havoc

    override val candidates: List<CandidateName> = buildCandidates {
        candidate {
            +nameType
        }
        candidate {
            +nameType
            +type.name
        }
    }

    override val children: List<AnyName> = listOf(nameType)
}
