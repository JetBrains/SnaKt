// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!VIPER_TEXT!>list_first_index_guarded<!>(values: List<Int>): Int {
    return if (values.size > 0) values[0] else 0
}

@AlwaysVerify
fun <!VIPER_TEXT!>list_last_index_guarded<!>(values: List<Int>): Int {
    return if (values.size > 0) values[values.size - 1] else 0
}

@AlwaysVerify
fun <!VIPER_TEXT!>list_index_below_zero<!>(values: List<Int>): Int = <!POSSIBLE_INDEX_OUT_OF_BOUND!>values[-1]<!>

@AlwaysVerify
fun <!VIPER_TEXT!>list_index_at_size<!>(values: List<Int>): Int = <!POSSIBLE_INDEX_OUT_OF_BOUND!>values[values.size]<!>

@AlwaysVerify
fun <!VIPER_TEXT!>empty_sublist_boundary<!>(): List<Int> = emptyList<Int>().subList(0, 0)

@AlwaysVerify
fun <!VIPER_TEXT!>mutable_list_after_one_add<!>(values: MutableList<Int>): Int {
    values.add(42)
    return values[values.size - 1]
}

@AlwaysVerify
fun <!VIPER_TEXT!>int_array_first_index_guarded<!>(values: IntArray): Int {
    return if (values.size > 0) values[0] else 0
}

@AlwaysVerify
fun <!VIPER_TEXT!>int_array_index_below_zero<!>(values: IntArray): Int = values[-1]

@AlwaysVerify
fun <!VIPER_TEXT!>generic_array_index_at_size<!>(values: Array<Int>): Int = values[values.size]
