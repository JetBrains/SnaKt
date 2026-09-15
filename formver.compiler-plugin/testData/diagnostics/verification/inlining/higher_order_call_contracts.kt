// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.verify

@NeverConvert
inline fun applyOnce(value: Int, callback: (Int) -> Int): Int = callback(value)

@NeverConvert
inline fun applyTwice(value: Int, callback: (Int) -> Int): Int = callback(callback(value))

@NeverConvert
inline fun applyConditionally(run: Boolean, value: Int, callback: (Int) -> Int): Int =
    if (run) callback(value) else value

@AlwaysVerify
fun <!VIPER_TEXT!>inlineCallbackOnce<!>() {
    val result = applyOnce(10) { it + 1 }
    verify(result == 11)
}

@AlwaysVerify
fun <!VIPER_TEXT!>inlineCallbackTwice<!>() {
    val result = applyTwice(10) { it + 1 }
    verify(result == 12)
}

@AlwaysVerify
fun <!VIPER_TEXT!>inlineCallbackTwiceNegativeControl<!>() {
    val result = applyTwice(10) { it + 1 }
    verify(<!VIPER_VERIFICATION_ERROR!>result == 11<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>inlineCallbackOnTakenBranch<!>(run: Boolean) {
    val result = applyConditionally(run, 10) { it + 1 }
    verify((run && result == 11) || (!run && result == 10))
}
