// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

@AlwaysVerify
fun <!VIPER_TEXT!>guardedGenericArrayRead<!>(array: Array<Int>, index: Int): Int {
    require(0 <= index && index < array.size)
    return array[index]
}

@AlwaysVerify
fun <!VIPER_TEXT!>guardedPrimitiveArrayRead<!>(array: IntArray, index: Int): Int {
    require(0 <= index && index < array.size)
    return array[index]
}

@AlwaysVerify
fun <!VIPER_TEXT!>emptyArraySize<!>() {
    val size = emptyArray<Int>().size
    verify(size == 0)
}

@AlwaysVerify
fun <!VIPER_TEXT!>constructedArrayRead<!>(): Int =
    IntArray(3)[2]

@AlwaysVerify
fun <!VIPER_TEXT!>constructedArrayLastRead<!>(n: Int): Int {
    require(n > 0)
    return IntArray(n)[n - 1]
}
