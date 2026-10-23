// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!VIPER_TEXT!>size_read_after_loop<!>(a: List<Int>, b: List<Int>): Int {
    var i = 0
    while (i < a.size) {
        i = i + 1
    }
    return b.size
}

@AlwaysVerify
fun <!VIPER_TEXT!>loop_not_touching_aliased_list<!>(l: List<Int>): Int {
    val m = l
    var i = 0
    while (i < 3) {
        i = i + 1
    }
    return i
}

@AlwaysVerify
fun <!VIPER_TEXT!>size_of_aliased_list<!>(l: List<Int>): Int {
    val m = l
    var i = 0
    while (i < m.size) {
        i = i + 1
    }
    return i + l.size
}

// Known gap: a loop reading a list through two aliases needs full permission to `size` through each.
@AlwaysVerify
fun <!VIPER_TEXT!>size_through_both_aliases<!>(l: List<Int>): Int {
    val m = l
    var i = 0
    <!VIPER_VERIFICATION_ERROR!>while (i < m.size && i < l.size) {
        i = i + 1
    }<!>
    return i
}
