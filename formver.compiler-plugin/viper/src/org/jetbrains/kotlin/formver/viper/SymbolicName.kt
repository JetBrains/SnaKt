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

    /**
     * True iff the name can appear in Viper.
     */
    val inViper: Boolean
}


/**
 * Represents a Kotlin name with its Viper equivalent.
 *
 * We could directly convert names and pass them around as strings, but this
 * approach makes it easier to see where they came from during debugging.
 */
const val SEPARATOR = "$"

interface SymbolicName : AnyName {
    val nameType: NameTypeBase?
        get() = null

}

context(nameResolver: NameResolver)
val SymbolicName.mangled: String
    get() = nameResolver.lookup(this)



/**
 * Collects all types of names we can have.
 *
 * Do not inherit from this interface. If you need a new name type, add it to the [NameType] interface.
 */
interface NameTypeBase : AnyName
