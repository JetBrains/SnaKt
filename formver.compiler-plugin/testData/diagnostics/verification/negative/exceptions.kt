// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.NeverConvert

@NeverConvert
fun observe(x: Int) {}

fun <!VIPER_TEXT!>tryCatchControl<!>() {
    try {
        observe(0)
    } catch (_: Exception) {
        observe(1)
    }
}

fun <!VIPER_TEXT!>tryFinally<!>() {
    try {
        observe(0)
    } finally {
        observe(1)
    }
}

fun <!VIPER_TEXT!>tryCatchFinally<!>() {
    try {
        observe(0)
    } catch (_: Exception) {
        observe(1)
    } finally {
        observe(2)
    }
}

fun explicitThrow() {
    <!INTERNAL_ERROR!>throw IllegalStateException()<!>
}
