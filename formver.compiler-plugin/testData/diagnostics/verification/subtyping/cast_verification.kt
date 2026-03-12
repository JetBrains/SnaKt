@file:Suppress("USELESS_IS_CHECK")

// ALWAYS_VALIDATE
// Probe: cast operators in verification context

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

open class Base(val x: Int)
class Derived(x: Int, val y: Int) : Base(x)

// Safe cast returns null for wrong type
fun <!VIPER_TEXT!>safeCastPreservesValue<!>(b: Base): Int {
    val d = b as? Derived
    return if (d != null) d.x else b.x
}

// Upcast should always succeed: verify Derived is Base
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>upcastAlwaysSucceeds<!>(d: Derived): Boolean {
    contract {
        returns(true)
    }
    return d is Base
}
