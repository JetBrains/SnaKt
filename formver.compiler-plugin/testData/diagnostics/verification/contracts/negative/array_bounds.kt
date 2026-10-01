// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!VIPER_TEXT!>uncheckedGenericArrayRead<!>(array: Array<Int>): Int =
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>array[0]<!>

@AlwaysVerify
fun <!VIPER_TEXT!>uncheckedPrimitiveArrayRead<!>(array: IntArray): Int =
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>array[0]<!>

@AlwaysVerify
fun <!VIPER_TEXT!>negativePrimitiveArrayRead<!>(array: IntArray): Int =
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>array[-1]<!>

@OptIn(ExperimentalUnsignedTypes::class)
@AlwaysVerify
fun <!VIPER_TEXT!>uncheckedUnsignedArrayRead<!>(array: UIntArray): UInt =
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>array[0]<!>

@AlwaysVerify
fun <!VIPER_TEXT!>emptyArrayRead<!>(): Int =
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>emptyArray<Int>()[0]<!>

@AlwaysVerify
fun <!VIPER_TEXT!>constructedArrayReadPastEnd<!>(): Int =
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>IntArray(3)[3]<!>

@AlwaysVerify
fun <!VIPER_TEXT!>uncheckedPrimitiveArrayWrite<!>(array: IntArray) {
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>array[0] = 1<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>negativeGenericArrayWrite<!>(array: Array<Int>) {
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>array[-1] = 1<!>
}
