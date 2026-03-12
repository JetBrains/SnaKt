// ALWAYS_VALIDATE
// Probe: what permissions does a constructor provide?

import org.jetbrains.kotlin.formver.plugin.Unique
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class Simple(val x: Int, var y: Int)

// After construction, we get unique ownership
// Can we pass the result to a @Unique parameter?
fun <!VIPER_TEXT!>takeUnique<!>(@Unique s: Simple): Int {
    return s.x
}

fun <!VIPER_TEXT!>constructAndPassUnique<!>(): Int {
    val s = Simple(1, 2)
    return takeUnique(s)
}

// Can we read val fields of freshly constructed object?
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>constructAndReadVal<!>(): Boolean {
    contract {
        returns(true)
    }
    val s = Simple(10, 20)
    return s.x == 10
}
