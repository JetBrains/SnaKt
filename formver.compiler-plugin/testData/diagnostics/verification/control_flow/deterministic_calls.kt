// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.verify

@NeverConvert
fun record(value: Int): Int = value

fun <!VIPER_TEXT!>nestedCallsBaseline<!>(flag: Boolean): Int {
    val first = record(record(1))
    val second = record(record(2))
    return if (flag) first else second
}

fun <!VIPER_TEXT!>nestedCallsPerturbed<!>(flag: Boolean): Int {
    val first = record(record(1))

    val second = record(record(2))
    return when (flag) {
        true -> first
        false -> second
    }
}

fun <!VIPER_TEXT!>recursiveBoundary<!>(remaining: Int): Int {
    if (remaining <= 0) return record(0)
    return record(recursiveBoundary(remaining - 1))
}

@AlwaysVerify
fun <!VIPER_TEXT!>positiveControl<!>(flag: Boolean) {
    val result = if (flag) 1 else 2
    verify(result == 1 || result == 2)
}

@AlwaysVerify
fun <!VIPER_TEXT!>negativeDiagnosticFirst<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>negativeDiagnosticSecond<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>1 == 2<!>)
}
