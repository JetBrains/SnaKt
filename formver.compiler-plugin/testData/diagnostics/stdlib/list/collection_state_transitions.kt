// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!VIPER_TEXT!>mutable_list_grows_step_by_step<!>(list: MutableList<Int>) {
    val initialSize = list.size

    list.add(10)
    list[initialSize]

    list.add(20)
    list[initialSize + 1]
    list[0]
}

@AlwaysVerify
fun <!VIPER_TEXT!>sublist_size_transitions<!>(list: List<Int>) {
    val initialSize = list.size

    val emptyPrefix = list.subList(0, 0)
    emptyPrefix.subList(0, 0)

    val wholeList = list.subList(0, list.size)
    wholeList.subList(0, initialSize)
}

@AlwaysVerify
fun <!VIPER_TEXT!>empty_list_stays_empty<!>() {
    val list = emptyList<Int>()
    list.subList(0, 0)
    list.subList(list.size, list.size)
}

@AlwaysVerify
fun <!VIPER_TEXT!>nonempty_prefix_is_singleton<!>(list: List<Int>) {
    if (list.isEmpty()) return

    val singleton = list.subList(0, 1)
    singleton[0]
    singleton.subList(0, singleton.size)
}

@AlwaysVerify
fun <!VIPER_TEXT!>one_add_does_not_make_second_index_safe<!>(list: MutableList<Int>) {
    val initialSize = list.size
    list.add(10)
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>list[initialSize + 1]<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>generic_array_read_at_zero<!>(array: Array<Int>) {
    if (array.isEmpty()) return

    array[0]
}

@AlwaysVerify
fun <!VIPER_TEXT!>primitive_array_read_at_zero<!>(array: IntArray) {
    if (array.isEmpty()) return

    array[0]
}

@AlwaysVerify
fun <!VIPER_TEXT!>generic_array_unchecked_read<!>(array: Array<Int>) {
    array[0]
}

@AlwaysVerify
fun <!VIPER_TEXT!>primitive_array_unchecked_read<!>(array: IntArray) {
    array[0]
}
