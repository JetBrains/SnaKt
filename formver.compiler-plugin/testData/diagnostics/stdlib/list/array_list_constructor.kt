// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!VIPER_TEXT!>empty_array_list<!>() : Int {
    val l = ArrayList<Int>()
    return l.size
}
