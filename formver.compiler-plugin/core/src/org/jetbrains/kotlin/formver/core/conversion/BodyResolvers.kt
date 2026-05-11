/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.formver.core.embeddings.FunctionBodyEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.FunctionExp
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Exp

/**
 * The output of converting an impure (method-style) function body: the `FunctionExp` wrap built
 * during conversion, together with the `ReturnTarget` it was converted against.
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
 * Stores per-function Viper outputs from the linearization phase, keyed by symbolic name.
 *
 * Each entry is written at most once; storing under a name that already has an entry is an error.
 */
class LinearizedBodyResolver {
    private val impure: MutableMap<SymbolicName, FunctionBodyEmbedding> = mutableMapOf()
    private val pure: MutableMap<SymbolicName, Exp> = mutableMapOf()

    fun storeImpure(name: SymbolicName, body: FunctionBodyEmbedding) {
        check(impure.put(name, body) == null) { "Linearized method body for $name was already stored" }
    }

    fun storePure(name: SymbolicName, body: Exp) {
        check(pure.put(name, body) == null) { "Linearized function body for $name was already stored" }
    }

    fun lookupImpure(name: SymbolicName): FunctionBodyEmbedding? = impure[name]
    fun lookupPure(name: SymbolicName): Exp? = pure[name]
}
