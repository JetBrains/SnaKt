// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!VIPER_TEXT!>safeFirstRepeated<!>(list: List<Int>): Int {
    return if (list.isEmpty()) 0 else list[0]
}

@AlwaysVerify
fun <!VIPER_TEXT!>safeFirstRepeatedAgain<!>(list: List<Int>): Int {
    return if (list.isEmpty()) 0 else list[0]
}

@AlwaysVerify
fun <!VIPER_TEXT!>safeFirstPerturbed<!>(list: List<Int>): Int {
    val firstIndex = 0
    return if (list.size > firstIndex) list[firstIndex] else 0
}

@AlwaysVerify
fun <!VIPER_TEXT!>mutableSingletonRepeated<!>(list: MutableList<Int>): Int {
    list.add(29)
    return list[0]
}

@AlwaysVerify
fun <!VIPER_TEXT!>mutableSingletonPerturbed<!>(list: MutableList<Int>): Int {
    val value = 29
    list.add(value)
    val firstIndex = 0
    return list[firstIndex]
}

@AlwaysVerify
fun <!VIPER_TEXT!>emptyBoundaryRepeated<!>(): Int = <!POSSIBLE_INDEX_OUT_OF_BOUND!>emptyList<Int>()[0]<!>

@AlwaysVerify
fun <!VIPER_TEXT!>emptyBoundaryPerturbed<!>(): Int {
    val empty: List<Int> = emptyList()
    val firstIndex = 0
    return <!POSSIBLE_INDEX_OUT_OF_BOUND!>empty[firstIndex]<!>
}
