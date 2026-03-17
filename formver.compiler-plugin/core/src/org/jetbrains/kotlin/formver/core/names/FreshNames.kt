/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.NameResolver
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
    override val mangledType: String
        get() = "anon"

    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { "anon" }
        yield { "anon$n" }
    }

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = n.toString()

    override fun dependsOn(): Set<SymbolicName> = emptySet()
}

data class AnonymousBuiltinName(val n: Int) : SymbolicName {

    override val mangledType: String
        get() = $$"anon$builtin"

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = n.toString()

    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield { $$"anon$builtin" }
        yield { $$"anon$builtin$$n" }
    }

    override fun dependsOn(): Set<SymbolicName> = emptySet()
}

/**
 * Name for return variable that should *only* be used in signatures of methods without a body.
 */
data object PlaceholderReturnVariableName : SymbolicName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "ret"

    override fun dependsOn(): Set<SymbolicName> = emptySet()
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield({ "ret" })
    }
}

data class ReturnVariableName(val n: Int) : SymbolicName {
    override val mangledType: String
        get() = "ret"

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = n.toString()

    override fun dependsOn(): Set<SymbolicName> = emptySet()
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield({ "ret" })
        yield({ "ret$n" })
    }
}

/**
 * Name for return variable that should *only* be used in signatures of pure functions
 * This variable will be translated into the special result variable in Viper
 */
data object FunctionResultVariableName : SymbolicName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "result"

    override fun dependsOn(): Set<SymbolicName> = emptySet()

    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield({ "result" })
    }
}

data object DispatchReceiverName : SymbolicName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = $$"this$dispatch"

    override fun dependsOn(): Set<SymbolicName> = emptySet()

    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield({ "this" })
        yield({ $$"this$dispatch" })
    }
}

data object ExtensionReceiverName : SymbolicName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = $$"this$extension"

    override fun dependsOn(): Set<SymbolicName> = emptySet()

    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield({ "this" })
        yield({ $$"this$extension" })
    }
}

data class SpecialName(val baseName: String) : SymbolicName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = baseName
    override val mangledType: String
        get() = "sp"

    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield({ baseName })
        yield({ "sp_${baseName}" })
    }

    override fun dependsOn(): Set<SymbolicName> = emptySet()
}

abstract class NumberedLabelName(val scope: String, val originalN: Int) : SymbolicName {
    override val mangledType: String
        get() = "lbl"

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = originalN.toString()

    context(nameResolver: NameResolver)
    override val mangledScope: String?
        get() = scope

    override fun dependsOn(): Set<SymbolicName> = emptySet()

    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield({ scope })
        yield({ "${scope}_$originalN" })
        yield({ "${mangledType}_${scope}_${originalN}" })
    }

}

data class ReturnLabelName(val scopeDepth: Int) : NumberedLabelName("ret", scopeDepth)
data class BreakLabelName(val n: Int) : NumberedLabelName("break", n)
data class ContinueLabelName(val n: Int) : NumberedLabelName("continue", n)
data class CatchLabelName(val n: Int) : NumberedLabelName("catch", n)
data class TryExitLabelName(val n: Int) : NumberedLabelName("try_exit", n)


data class PlaceholderArgumentName(val n: Int) : SymbolicName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "arg$n"
    override fun dependsOn(): Set<SymbolicName> = emptySet()

    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield({ "arg" })
        yield({ "arg$n" })
    }
}

data class DomainFuncParameterName(val baseName: String) : SymbolicName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = baseName

    override fun dependsOn(): Set<SymbolicName> = emptySet()
    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield({ baseName })
    }
}

data class SsaVariableName(val ssaIndex: Int, val baseName: SymbolicName) : SymbolicName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "${baseName.mangled}$$ssaIndex"

    override fun dependsOn(): Set<SymbolicName> = emptySet()

    override val candidates: Sequence<(NameResolver) -> String> = sequence {
        yield({ resolver -> resolver.resolve(baseName) })
        yield({ resolver -> "${resolver.resolve(baseName)}${ssaIndex}" })
    }
}