// ALWAYS_VALIDATE

import org.jetbrains.kotlin.formver.plugin.postconditions

fun <!VIPER_TEXT!>return_null<!>(): Int? = null

fun <!VIPER_TEXT!>nullablePassthrough<!>(x: Int?): Int? {
    postconditions<Int?> {
        it == x
    }
    return x
}

fun <!VIPER_TEXT!>elvisWithNonNull<!>(x: Int): Int {
    postconditions<Int> {
        it == x
    }
    val y: Int? = x
    return y ?: 0
}

fun <!VIPER_TEXT!>smartcastInBranch<!>(n: Int?): Int {
    if (n != null) {
        return n
    }
    return 0
}
