// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

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
