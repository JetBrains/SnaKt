// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>deMorganConjunction<!>(a: Boolean, b: Boolean): Boolean {
    postconditions<Boolean> { result -> result == (!a || !b) }
    return !(a && b)
}

@AlwaysVerify
fun <!VIPER_TEXT!>deMorganDisjunction<!>(a: Boolean, b: Boolean): Boolean {
    postconditions<Boolean> { result -> result == (!a && !b) }
    return !(a || b)
}

@AlwaysVerify
fun <!VIPER_TEXT!>associatedConjunction<!>(a: Boolean, b: Boolean, c: Boolean): Boolean {
    postconditions<Boolean> { result -> result == (a && (b && c)) }
    return (a && b) && c
}

@AlwaysVerify
fun <!VIPER_TEXT!>associatedDisjunction<!>(a: Boolean, b: Boolean, c: Boolean): Boolean {
    postconditions<Boolean> { result -> result == (a || (b || c)) }
    return (a || b) || c
}

@AlwaysVerify
fun <!VIPER_TEXT!>negatedLessThan<!>(x: Int, y: Int): Boolean {
    postconditions<Boolean> { result -> result == (x >= y) }
    return !(x < y)
}

@AlwaysVerify
fun <!VIPER_TEXT!>negatedComparisonChain<!>(x: Int, y: Int, z: Int): Boolean {
    postconditions<Boolean> { result -> result == (x >= y || y >= z) }
    return !(x < y && y < z)
}

@NeverVerify
fun <!VIPER_TEXT!>strictComparisonBoundary<!>(x: Int, y: Int): Boolean {
    postconditions<Boolean> { result -> result == (x > y) }
    return !(x < y)
}

@NeverVerify
fun <!VIPER_TEXT!>incorrectDeMorganBoundary<!>(a: Boolean, b: Boolean): Boolean {
    postconditions<Boolean> { result -> result == (!a && !b) }
    return !(a && b)
}
