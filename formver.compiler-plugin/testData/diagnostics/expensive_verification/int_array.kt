// FULL_JDK
// USE_STDLIB
// FULL_VIPER_DUMP

import org.jetbrains.kotlin.formver.plugin.*


fun <!VIPER_TEXT!>testConstructor<!>() {
    val array = IntArray(5)
    val test = array[0] == 0
    verify(test)
}
