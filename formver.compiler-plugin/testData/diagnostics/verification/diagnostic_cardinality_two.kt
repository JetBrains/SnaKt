// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>firstVerificationDiagnostic<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>secondVerificationDiagnostic<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}
