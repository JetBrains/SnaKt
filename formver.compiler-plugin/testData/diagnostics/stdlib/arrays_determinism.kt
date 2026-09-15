// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!VIPER_TEXT!>arrayReadRepeated<!>(values: Array<Int>): Int = values[0]

@AlwaysVerify
fun <!VIPER_TEXT!>arrayReadPerturbed<!>(values: Array<Int>): Int {
    val firstIndex = 0
    return values[firstIndex]
}

@AlwaysVerify
fun <!VIPER_TEXT!>arraySizeRepeated<!>(values: Array<Int>): Int = values.size

@AlwaysVerify
fun <!VIPER_TEXT!>arraySizePerturbed<!>(values: Array<Int>): Int {
    val observedSize = values.size
    return observedSize
}

@AlwaysVerify
fun <!VIPER_TEXT!>primitiveArrayReadRepeated<!>(values: IntArray): Int = values[0]

@AlwaysVerify
fun <!VIPER_TEXT!>primitiveArrayReadPerturbed<!>(values: IntArray): Int {
    val firstIndex = 0
    return values[firstIndex]
}

@AlwaysVerify
fun <!VIPER_TEXT!>primitiveArraySize<!>(values: IntArray): Int = values.size

@AlwaysVerify
fun <!VIPER_TEXT!>emptyArrayBoundary<!>(): Int = emptyArray<Int>()[0]
