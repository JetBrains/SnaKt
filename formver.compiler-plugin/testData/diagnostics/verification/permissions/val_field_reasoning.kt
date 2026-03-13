// ALWAYS_VALIDATE
// Probe: can we reason about val field values?

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class Pair(val first: Int, val second: Int)

// Can we prove a property about val fields?
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>valFieldPreserved<!>(p: Pair): Boolean {
    contract {
        returns(true)
    }
    val x = p.first
    val y = p.first
    return x == y
}

// Can we prove constructor-provided value is retained?
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>constructorValueRetained<!>(): Boolean {
    contract {
        returns(true)
    }
    val p = Pair(1, 2)
    return p.first == 1 && p.second == 2
}
