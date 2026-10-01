// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!VIPER_TEXT!>list_positive_control<!>(items: List<Int>): Int {
    return if (items.isEmpty()) 0 else items[items.size - 1]
}

@AlwaysVerify
fun <!VIPER_TEXT!>list_negative_index<!>(items: List<Int>): Int {
    return <!POSSIBLE_INDEX_OUT_OF_BOUND!>items[-1]<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>list_upper_bound<!>(items: List<Int>): Int {
    return <!POSSIBLE_INDEX_OUT_OF_BOUND!>items[items.size]<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>mutable_list_boundary_after_add<!>(items: MutableList<Int>) {
    items.add(1)
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>items[items.size]<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>array_positive_control<!>(items: Array<Int>): Int {
    return if (items.isEmpty()) 0 else items[items.size - 1]
}

@AlwaysVerify
fun <!VIPER_TEXT!>array_upper_bound<!>(items: Array<Int>): Int {
    return items[items.size]
}

@AlwaysVerify
fun <!VIPER_TEXT!>empty_array_index<!>(): Int {
    return emptyArray<Int>()[0]
}

@AlwaysVerify
fun <!VIPER_TEXT!>int_array_positive_control<!>(items: IntArray): Int {
    return if (items.isEmpty()) 0 else items[0]
}

@AlwaysVerify
fun <!VIPER_TEXT!>int_array_negative_index<!>(items: IntArray): Int {
    return items[-1]
}
