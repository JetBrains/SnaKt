// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!VIPER_TEXT!>reference_array_size<!>(values: Array<Int>): Int = values.size

@AlwaysVerify
fun <!VIPER_TEXT!>reference_array_get<!>(values: Array<Int>): Int = values[0]

// Native-array get has no bounds model: impossible indices currently verify.
@AlwaysVerify
fun <!VIPER_TEXT!>empty_reference_array_get_zero<!>(): Int = emptyArray<Int>()[0]

@AlwaysVerify
fun <!VIPER_TEXT!>reference_array_get_negative_one<!>(values: Array<Int>): Int = values[-1]

@AlwaysVerify
fun <!VIPER_TEXT!>primitive_array_size<!>(values: IntArray): Int = values.size

@AlwaysVerify
fun <!VIPER_TEXT!>primitive_array_get<!>(values: IntArray): Int = values[0]

@AlwaysVerify
fun <!VIPER_TEXT!>empty_primitive_array_get_zero<!>(): Int = intArrayOf()[0]

@AlwaysVerify
fun <!VIPER_TEXT!>primitive_array_get_size<!>(values: IntArray): Int = values[values.size]
