/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper

/**
 * Interface to unify all structures that are names to something.
 */
interface AnyName {
    fun register(nameResolver: NameResolver) {
        nameResolver.register(this)
    }
}


/**
 * Represents a Kotlin name with its Viper equivalent.
 *
 * We could directly convert names and pass them around as strings, but this
 * approach makes it easier to see where they came from during debugging.
 */
const val SEPARATOR = "$"

interface SymbolicName : AnyName {
    val nameType: NameType?
        get() = null
}

context(nameResolver: NameResolver)
val SymbolicName.mangled: String
    get() = nameResolver.lookup(this)


/**
 * Collects all types of names we can have.
 */
sealed interface NameType : AnyName {

    enum class Member : NameType {
        Property, BackingField, Getter, Setter, ExtensionSetter, ExtensionGetter
    }

    enum class Base : NameType {
        Constructor, Function, Predicate, Havoc, Variable, Domain, DomainFunction, Label
    }

    enum class TypeCategory : NameType {
        Class, GeneralType
    }

}