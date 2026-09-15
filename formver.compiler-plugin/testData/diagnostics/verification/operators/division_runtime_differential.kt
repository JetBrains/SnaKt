// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

@AlwaysVerify
fun <!VIPER_TEXT!>positiveDivisionControl<!>(): Int {
    val result = 84 / 2
    verify(result == 42)
    return result
}

@AlwaysVerify
fun <!VIPER_TEXT!>divisionByZeroBoundary<!>(): Int {
    val zero = 0
    return <!VIPER_VERIFICATION_ERROR!>1 / zero<!>
}
