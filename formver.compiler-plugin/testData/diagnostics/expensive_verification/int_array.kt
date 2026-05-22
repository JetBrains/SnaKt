// FULL_JDK
// USE_STDLIB
// FULL_VIPER_DUMP

import org.jetbrains.kotlin.formver.plugin.*


fun <!VIPER_TEXT!>testConstructor<!>(): IntArray {
    postconditions<IntArray> {
        res

        forAll<Int> {
            (0 <= it && it <= 4) implies res[it] < res[it + 1]
        }
    }

    val array = IntArray(5)
    array.set(1, 1)
    array.set(2, 2)
    array.set(3, 2)
    array.set(4, 2)
    return array


}
