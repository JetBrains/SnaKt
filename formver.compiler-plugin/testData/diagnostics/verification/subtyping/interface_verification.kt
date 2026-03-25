@file:Suppress("USELESS_IS_CHECK")

// ALWAYS_VALIDATE
// Probe: interface-based subtyping verification

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

interface HasName {
    val name: String
}

interface HasAge {
    val age: Int
}

class Person(override val name: String, override val age: Int) : HasName, HasAge

// Verify accessing interface properties through implementing class
fun <!VIPER_TEXT!>getPersonName<!>(p: Person): String {
    return p.name
}

// Verify subtype relationship: Person is HasName
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>personIsHasName<!>(p: Person): Boolean {
    contract {
        returns(true)
    }
    return p is HasName
}
