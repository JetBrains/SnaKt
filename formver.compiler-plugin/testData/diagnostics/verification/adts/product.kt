// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@ADT
data class Pair(val a: Int, val b: Int)

@AlwaysVerify
fun <!VIPER_TEXT!>testConstrAndDestr<!>() {
    val a = 1
    val b = 2
    val p = Pair(a, b)
    val a_p = p.a
    val b_p = p.b
    verify(a == a_p)
    verify(b == b_p)
}

@AlwaysVerify
@Suppress("useless_is_check")
fun <!VIPER_TEXT!>destrGivenAdt<!>(pair: Pair) {
    verify(pair.a is Int)
    verify(pair.b is Int)
}

@AlwaysVerify
fun <!VIPER_TEXT!>adtInPostcondition<!>(p1: Pair): Pair {
    postconditions<Pair> { p1.b == it.b }
    return Pair(0, p1.b)
}
