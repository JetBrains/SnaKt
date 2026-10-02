/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.formver.core.embeddings.callables.NamedFunctionSignature
import org.jetbrains.kotlin.formver.core.names.NameMatcher

/**
 * Identifies the stdlib function a stdlib contract belongs to.
 * Every pattern fixes the package, the receiver, the name and the number of value parameters,
 * so a user function that only shares the simple name does not match.
 */
sealed interface StdLibFunctionPattern {
    fun matches(function: NamedFunctionSignature, typeResolver: TypeResolver): Boolean
}

/**
 * A member function [name] with [arity] value parameters whose dispatch receiver is a subtype of `[pkg].[className]`.
 * Overrides in user classes match, since they are bound by the stdlib contract.
 */
data class MemberOf(val pkg: List<String>, val className: String, val name: String, val arity: Int) :
    StdLibFunctionPattern {
    override fun matches(function: NamedFunctionSignature, typeResolver: TypeResolver): Boolean {
        val receiverType = function.callableType.dispatchReceiverType ?: return false
        return function.callableType.extensionReceiverType == null &&
                function.params.size == arity &&
                function.hasFunctionName(name) &&
                typeResolver.isInheritorOf(receiverType.pretype, pkg, className)
    }
}

/** A top-level function [name] in [pkg] with [arity] value parameters and no receivers. */
data class TopLevel(val pkg: List<String>, val name: String, val arity: Int) : StdLibFunctionPattern {
    override fun matches(function: NamedFunctionSignature, typeResolver: TypeResolver): Boolean {
        if (function.callableType.dispatchReceiverType != null) return false
        if (function.callableType.extensionReceiverType != null) return false
        if (function.params.size != arity || !function.hasFunctionName(name)) return false
        NameMatcher.matchClassScope(function.name) {
            ifNoReceiver {
                ifPackageName(pkg) { return true }
            }
            return false
        }
    }
}

private fun NamedFunctionSignature.hasFunctionName(name: String): Boolean {
    NameMatcher.matchClassScope(this.name) {
        ifFunctionName(name) { return true }
        return false
    }
}
