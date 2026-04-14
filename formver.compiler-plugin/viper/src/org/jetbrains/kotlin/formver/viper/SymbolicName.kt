/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper

/**
 * Represents a Kotlin name with its Viper equivalent.
 *
 * We could directly convert names and pass them around as strings, but this
 * approach makes it easier to see where they came from during debugging.
 */
const val SEPARATOR = "$"
interface SymbolicName {
    val mangledType: NameType?
        get() = null
    context(nameResolver: NameResolver)
    val mangledScope: String?
        get() = null
    context(nameResolver: NameResolver)
    val mangledBaseName: String
}

context(nameResolver: NameResolver)
val SymbolicName.mangled: String
    get() = nameResolver.resolve(this)

val SymbolicName.debugMangled: String
    get() {
        val debugResolver = DebugNameResolver()
        return debugResolver.resolve(this)
    }


/**
 * Collects all types of names we can have.
 */
sealed class NameType(val name: String) {

    override fun toString(): String = name

    object Property : NameType("p")
    object BackingField : NameType("bf")
    object Getter : NameType("g")
    object Setter : NameType("s")
    object ExtensionSetter : NameType("es")
    object ExtensionGetter : NameType("eg")
    object Type : NameType("t") {
        object Class : NameType("c")
    }

    object Constructor : NameType("con")
    object Function : NameType("f")
    object Predicate : NameType("pred")
    object Havoc : NameType("havoc")
    object Label : NameType("lbl")
    object Variable : NameType("v")
    object Domain : NameType("d")
    object DomainFunction : NameType("df")
    object Special : NameType("sp") // I think we should not have this. Like, what does special mean?
    object Adt : NameType("adt")
    object AdtCons : NameType("adtc")
}