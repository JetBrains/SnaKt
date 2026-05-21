// FULL_JDK
// USE_STDLIB
// FULL_VIPER_DUMP

import org.jetbrains.kotlin.formver.plugin.*


fun <!VIPER_TEXT!>testConstructor<!>() {
    val array = IntArray(5)
    verify(array.size == 5)
}
