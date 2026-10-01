// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!VIPER_TEXT!>mutable_list_first_after_add<!>(xs: MutableList<Int>): Int {
    xs.add(7)
    return xs[0]
}

@AlwaysVerify
fun <!VIPER_TEXT!>mutable_list_at_size_after_add<!>(xs: MutableList<Int>): Int {
    xs.add(7)
    return <!POSSIBLE_INDEX_OUT_OF_BOUND!>xs[xs.size]<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>guarded_last<!>(xs: List<Int>): Int? {
    return if (xs.isEmpty()) null else xs[xs.size - 1]
}

@AlwaysVerify
fun <!VIPER_TEXT!>guarded_at_size<!>(xs: List<Int>): Int? {
    return if (xs.isEmpty()) null else <!POSSIBLE_INDEX_OUT_OF_BOUND!>xs[xs.size]<!>
}
