// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.preconditions

@AlwaysVerify
fun <!VIPER_TEXT!>size_probe<!>(list: List<Int>) {
    val size = list.size
}

@AlwaysVerify
fun <!VIPER_TEXT!>empty_list_probe<!>() {
    val list = emptyList<Int>()
}

@AlwaysVerify
fun <!VIPER_TEXT!>is_empty_probe<!>(list: List<Int>) {
    val empty = list.isEmpty()
}

<!INTERNAL_ERROR!>@AlwaysVerify
fun size_precondition_probe(list: List<Int>) {
    preconditions { list.size > 0 }
}<!>

@AlwaysVerify
fun <!VIPER_TEXT!>zero_index_may_be_invalid<!>(list: List<Int>) {
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>list[0]<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>negative_index_is_invalid<!>(list: List<Int>) {
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>list[-1]<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>indices_probe<!>(list: List<Int>) {
    val range = list.indices
}

@AlwaysVerify
fun <!VIPER_TEXT!>first_probe<!>(list: List<Int>) {
    list.first()
}

@AlwaysVerify
fun <!VIPER_TEXT!>last_probe<!>(list: List<Int>) {
    list.last()
}

@AlwaysVerify
fun <!VIPER_TEXT!>list_of_probe<!>() {
    val list = listOf(1)
}
