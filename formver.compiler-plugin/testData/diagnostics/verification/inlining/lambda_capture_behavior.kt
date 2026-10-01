// FULL_JDK
// REPLACE_STDLIB_EXTENSIONS

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.verify

@NeverConvert
inline fun <T> invokeImmediately(block: () -> T): T = block()

@AlwaysVerify
fun <!VIPER_TEXT!>immediateImmutableCapture<!>() {
    val captured = 41
    val result = invokeImmediately { captured + 1 }
    verify(result == 42)
}

@AlwaysVerify
fun <!VIPER_TEXT!>immediateImmutableCaptureNegative<!>() {
    val captured = 41
    val result = invokeImmediately { captured + 1 }
    verify(<!VIPER_VERIFICATION_ERROR!>result == 41<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>immediateMutableCapture<!>() {
    var captured = 40
    val result = invokeImmediately {
        captured += 2
        captured
    }
    verify(result == 42, captured == 42)
}

class CaptureReceiver(val value: Int) {
    @AlwaysVerify
    fun <!VIPER_TEXT!>immediateReceiverCapture<!>() {
        val result = invokeImmediately { this.value }
        verify(result == value)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>immediateBranchRefinedCapture<!>(value: Any) {
    if (value is Int) {
        val result = invokeImmediately { value + 1 }
        verify(result > value)
    }
}
