// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

@AlwaysVerify
fun <!VIPER_TEXT!>object_array_first<!>(xs: Array<Int>): Int? {
    return if (xs.isEmpty()) null else xs[0]
}

@AlwaysVerify
fun <!VIPER_TEXT!>object_array_at_size<!>(xs: Array<Int>): Int? {
    return if (xs.isEmpty()) null else xs[xs.size]
}

@AlwaysVerify
fun <!VIPER_TEXT!>primitive_array_first<!>(xs: IntArray): Int? {
    return if (xs.isEmpty()) null else xs[0]
}

@AlwaysVerify
fun <!VIPER_TEXT!>primitive_array_at_size<!>(xs: IntArray): Int? {
    return if (xs.isEmpty()) null else xs[xs.size]
}

@AlwaysVerify
fun <!VIPER_TEXT!>object_empty_array_size<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>emptyArray<Int>().size == 0<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>primitive_empty_array_size<!>() {
    verify(intArrayOf().size == 0)
}
