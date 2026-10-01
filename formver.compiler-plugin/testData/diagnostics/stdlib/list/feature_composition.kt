// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!VIPER_TEXT!>add_then_first<!>(list: MutableList<Int>) {
    list.add(7)
    list[0]
}

@AlwaysVerify
fun <!VIPER_TEXT!>add_then_exact_size<!>(list: MutableList<Int>) {
    list.add(7)
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>list[list.size]<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>negative_list_index<!>(list: List<Int>) {
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>list[-1]<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>guarded_array_read<!>(array: Array<Int>) {
    if (array.isNotEmpty()) {
        array[0]
    }
}

@AlwaysVerify
fun singleton_array_write() {
    val array = arrayOf(<!INTERNAL_ERROR!>1<!>)
    array[0] = 2
}

@AlwaysVerify
fun <!VIPER_TEXT!>empty_int_array_read<!>() {
    val array = intArrayOf()
    array[0]
}
