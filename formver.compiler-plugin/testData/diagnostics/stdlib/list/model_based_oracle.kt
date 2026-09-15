// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

// Oracle: size == 0 implies no valid index in {-1, 0}.
@AlwaysVerify
fun <!VIPER_TEXT!>empty_size_is_zero<!>(): Int {
    val values = emptyList<Int>()
    return values.size
}

@AlwaysVerify
fun <!VIPER_TEXT!>empty_rejects_zero<!>() {
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>emptyList<Int>()[0]<!>
}

// listOf has no singleton-size model yet, so valid index 0 remains unprovable.
@AlwaysVerify
fun <!VIPER_TEXT!>singleton_nonempty_is_unmodeled<!>() {
    val values = listOf(7)
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>values[0]<!>
}

// Oracle: size > 0 makes index 0 valid; -1 and size stay outside [0, size).
@AlwaysVerify
fun <!VIPER_TEXT!>nonempty_accepts_zero<!>(values: List<Int>): Int {
    if (values.isEmpty()) return 0
    return values[0]
}

@AlwaysVerify
fun <!VIPER_TEXT!>any_size_rejects_negative_one<!>(values: List<Int>) {
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>values[-1]<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>any_size_rejects_size<!>(values: List<Int>) {
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>values[values.size]<!>
}

// Oracle: add changes size n to n + 1, so old n is valid and new size is not.
@AlwaysVerify
fun <!VIPER_TEXT!>add_accepts_previous_size<!>(values: MutableList<Int>): Int {
    val previousSize = values.size
    values.add(7)
    return values[previousSize]
}

@AlwaysVerify
fun <!VIPER_TEXT!>add_rejects_new_size<!>(values: MutableList<Int>) {
    values.add(7)
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>values[values.size]<!>
}
