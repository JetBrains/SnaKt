/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.FunctionExp
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Function
import org.jetbrains.kotlin.formver.viper.ast.Method

/**
 * The output of converting an impure (method-style) function body.
 *
 * The [returnTarget] is preserved alongside [bodyExp] because linearization needs to attach a
 * Unit invariant to the return variable when the function returns Unit and the body has no
 * explicit `return`. The body alone does not expose that variable, so conversion has to hand
 * it off explicitly.
 */
data class ConvertedMethodBody(
    val bodyExp: FunctionExp,
    val returnTarget: ReturnTarget,
)

/**
 * Stores per-function `ExpEmbedding` outputs from the conversion phase, keyed by symbolic name.
 * Pure and impure functions live in separate maps so the value type can stay precise.
 *
 * Each entry is written at most once; storing under a name that already has an entry is an error.
 */
class ConvertedBodyResolver {
    private val impure: MutableMap<SymbolicName, ConvertedMethodBody> = mutableMapOf()
    private val pure: MutableMap<SymbolicName, ExpEmbedding> = mutableMapOf()

    fun storeImpure(name: SymbolicName, body: ConvertedMethodBody) {
        check(impure.put(name, body) == null) { "Converted method body for $name was already stored" }
    }

    fun storePure(name: SymbolicName, body: ExpEmbedding) {
        check(pure.put(name, body) == null) { "Converted function body for $name was already stored" }
    }

    fun lookupImpure(name: SymbolicName): ConvertedMethodBody? = impure[name]
    fun lookupPure(name: SymbolicName): ExpEmbedding? = pure[name]

    fun forEachImpure(action: (SymbolicName, ConvertedMethodBody) -> Unit) {
        impure.forEach { (name, body) -> action(name, body) }
    }

    fun forEachPure(action: (SymbolicName, ExpEmbedding) -> Unit) {
        pure.forEach { (name, body) -> action(name, body) }
    }
}

/**
 * Stores the finalized Viper [Method] / [Function] for each user-defined or partially-special callable,
 * keyed by symbolic name. Populated by `linearizeAll` so that `buildProgram` reduces to concatenation.
 *
 * Iteration order over [methods] / [functions] reflects insertion order, which is the order the
 * `ProgramConverter` emits.
 */
class LinearizedBodyResolver {
    private val methodsByName: MutableMap<SymbolicName, Method> = mutableMapOf()
    private val functionsByName: MutableMap<SymbolicName, Function> = mutableMapOf()

    fun storeMethod(name: SymbolicName, method: Method) {
        check(methodsByName.put(name, method) == null) { "Linearized method for $name was already stored" }
    }

    fun storeFunction(name: SymbolicName, function: Function) {
        check(functionsByName.put(name, function) == null) { "Linearized function for $name was already stored" }
    }

    val methods: Collection<Method> get() = methodsByName.values
    val functions: Collection<Function> get() = functionsByName.values
}
