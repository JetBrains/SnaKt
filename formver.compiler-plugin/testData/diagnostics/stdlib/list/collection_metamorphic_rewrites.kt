// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!VIPER_TEXT!>guarded_list_not_empty<!>(list: List<Int>): Int {
    return if (!list.isEmpty()) list[0] else -1
}

@AlwaysVerify
fun <!VIPER_TEXT!>guarded_list_false_comparison<!>(list: List<Int>): Int {
    return if (list.isEmpty() == false) list.get(0) else -1
}

@AlwaysVerify
fun <!VIPER_TEXT!>empty_list_inline<!>(): Int = <!POSSIBLE_INDEX_OUT_OF_BOUND!>emptyList<Int>()[0]<!>

@AlwaysVerify
fun <!VIPER_TEXT!>empty_list_named<!>(): Int {
    val renamed: List<Int> = emptyList()
    return <!POSSIBLE_INDEX_OUT_OF_BOUND!>renamed.get(0)<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>mutable_add_literal<!>(list: MutableList<Int>): Int {
    list.add(1)
    return list[0]
}

@AlwaysVerify
fun <!VIPER_TEXT!>mutable_add_named<!>(list: MutableList<Int>): Int {
    val element = 1
    list.add(element)
    return list.get(0)
}

@AlwaysVerify
fun <!VIPER_TEXT!>mutable_set_not_empty<!>(list: MutableList<Int>, value: Int) {
    if (!list.isEmpty()) list.set(0, value)
}

@AlwaysVerify
fun <!VIPER_TEXT!>mutable_set_false_comparison<!>(list: MutableList<Int>, value: Int) {
    if (list.isEmpty() == false) list.set(0, value)
}

@AlwaysVerify
fun <!VIPER_TEXT!>mutable_set_unchecked<!>(list: MutableList<Int>, value: Int) {
    list.set(0, value)
}

@AlwaysVerify
fun <!VIPER_TEXT!>object_array_operator<!>(array: Array<Int>): Int = array[0]

@AlwaysVerify
fun <!VIPER_TEXT!>object_array_call<!>(renamed: Array<Int>): Int = renamed.get(0)

@AlwaysVerify
fun <!VIPER_TEXT!>primitive_array_operator<!>(array: IntArray): Int = array[0]

@AlwaysVerify
fun <!VIPER_TEXT!>primitive_array_call<!>(renamed: IntArray): Int = renamed.get(0)
