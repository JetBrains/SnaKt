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
fun <!VIPER_TEXT!>guardedPrimitiveArrayWrite<!>(array: IntArray, index: Int) {
    require(0 <= index && index < array.size)
    val before = array.size
    array[index] = 1
    val after = array.size
    verify(before == after)
}
