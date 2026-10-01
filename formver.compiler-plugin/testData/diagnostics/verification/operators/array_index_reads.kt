// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!VIPER_TEXT!>read_literal_zero<!>(a: IntArray): Int = a[0]

@AlwaysVerify
fun <!VIPER_TEXT!>read_computed_zero<!>(a: IntArray): Int = a[1 - 1]

@AlwaysVerify
fun <!VIPER_TEXT!>read_last<!>(a: IntArray): Int = a[a.size - 1]

@AlwaysVerify
fun <!VIPER_TEXT!>read_computed_last<!>(a: IntArray): Int = a[(a.size + 1) - 2]

@AlwaysVerify
fun <!VIPER_TEXT!>read_empty_array<!>(): Int = IntArray(0)[0]

@AlwaysVerify
fun <!VIPER_TEXT!>read_after_bounds_refinement<!>(a: IntArray): Int {
    if (a.size > 0) {
        return a[0] + a[1 - 1]
    }
    return 0
}

@AlwaysVerify
fun <!VIPER_TEXT!>read_one_past_end<!>(a: IntArray): Int {
    if (a.size > 0) {
        return a[a.size]
    }
    return 0
}
