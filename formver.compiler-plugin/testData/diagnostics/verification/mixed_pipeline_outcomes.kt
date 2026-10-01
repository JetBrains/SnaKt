// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.NeverVerify
import org.jetbrains.kotlin.formver.plugin.verify

@NeverVerify
fun <!VERIFICATION_SKIPPED!>conversionFailure<!>() {
    var x = 0
    verify(<!PURITY_VIOLATION!>++x == 1<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>positiveControl<!>(x: Int) {
    verify(x == x)
}

@AlwaysVerify
fun <!VIPER_TEXT!>verificationFailure<!>(x: Int) {
    verify(<!VIPER_VERIFICATION_ERROR!>x > 0<!>)
}
