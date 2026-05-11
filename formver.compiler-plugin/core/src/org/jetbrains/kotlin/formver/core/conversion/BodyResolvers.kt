/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.formver.core.embeddings.FunctionBodyEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Exp

/**
 * The output of converting an impure function body: the fully-built `ExpEmbedding` (typically a
 * `FunctionExp` wrapping the body) together with the `ReturnTarget` it was converted against.
 */
data class ImpureConvertedBody(
    val bodyExp: ExpEmbedding,
    val returnTarget: ReturnTarget,
)

/**
 * Stores per-function `ExpEmbedding` outputs from the conversion phase, keyed by symbolic name.
 * Pure and impure functions live in separate maps so the value type can stay precise.
 */
class ConvertedBodyResolver {
    private val impure: MutableMap<SymbolicName, ImpureConvertedBody> = mutableMapOf()
    private val pure: MutableMap<SymbolicName, ExpEmbedding> = mutableMapOf()

    fun storeImpure(name: SymbolicName, body: ImpureConvertedBody) {
        impure[name] = body
    }

    fun storePure(name: SymbolicName, body: ExpEmbedding) {
        pure[name] = body
    }

    fun lookupImpure(name: SymbolicName): ImpureConvertedBody? = impure[name]
    fun lookupPure(name: SymbolicName): ExpEmbedding? = pure[name]

    fun forEachImpure(action: (SymbolicName, ImpureConvertedBody) -> Unit) {
        impure.forEach { (name, body) -> action(name, body) }
    }

    fun forEachPure(action: (SymbolicName, ExpEmbedding) -> Unit) {
        pure.forEach { (name, body) -> action(name, body) }
    }
}

/**
 * Stores per-function Viper outputs from the linearization phase, keyed by symbolic name.
 * Bodies that fail validity checks have no entry here.
 */
class LinearizedBodyResolver {
    private val impure: MutableMap<SymbolicName, FunctionBodyEmbedding> = mutableMapOf()
    private val pure: MutableMap<SymbolicName, Exp> = mutableMapOf()

    fun storeImpure(name: SymbolicName, body: FunctionBodyEmbedding) {
        impure[name] = body
    }

    fun storePure(name: SymbolicName, body: Exp) {
        pure[name] = body
    }

    fun lookupImpure(name: SymbolicName): FunctionBodyEmbedding? = impure[name]
    fun lookupPure(name: SymbolicName): Exp? = pure[name]
}
