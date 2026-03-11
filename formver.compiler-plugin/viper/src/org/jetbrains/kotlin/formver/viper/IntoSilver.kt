/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper

import scala.Option

/**
 * Conversion interface from the Kotlin-side Viper AST to the Silver (`viper.silver.ast`) AST
 * consumed directly by the Silicon verifier.
 *
 * Every node in the Viper AST hierarchy implements this interface.  The [NameResolver] context
 * receiver is required so that [SymbolicName] instances (which encode Kotlin-level names) can be
 * mangled into globally unique Silver-level identifiers during conversion.
 *
 * @param T the corresponding Silver AST node type produced by [toSilver].
 */
interface IntoSilver<out T> {
    context(nameResolver: NameResolver)
    fun toSilver(): T
}

/** Converts a Scala [Option] of an [IntoSilver] value to an [Option] of its Silver counterpart. */
context(nameResolver: NameResolver)
fun <T, V> Option<T>.toSilver(): Option<V> where T : IntoSilver<V> = map { it.toSilver() }

/** Converts a [List] of [IntoSilver] values to a list of their Silver counterparts. */
context(nameResolver: NameResolver)
fun <T, V> List<T>.toSilver(): List<V> where T : IntoSilver<V> =
    map { it.toSilver() }

/** Converts a [Map] whose keys and values both implement [IntoSilver] to a map of their Silver counterparts. */
context(nameResolver: NameResolver)
fun <K, V, K2, V2> Map<K, V>.toSilver(): Map<K2, V2> where K : IntoSilver<K2>, V : IntoSilver<V2> =
    this.mapKeys { it.key.toSilver() }.mapValues { it.value.toSilver() }