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

@AlwaysVerify
fun <!VIPER_TEXT!>emptyArrayRead<!>(): Int =
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>emptyArray<Int>()[0]<!>

@AlwaysVerify
fun <!VIPER_TEXT!>constructedArrayReadPastEnd<!>(): Int =
    <!POSSIBLE_INDEX_OUT_OF_BOUND!>IntArray(3)[3]<!>
