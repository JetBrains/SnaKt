// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>verify_false<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>verify_compound<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>true && false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>verify_forall<!>() {
    verify(
        forAll<Int> { i ->
            i > 0 || i < 0 || i == 0
        }
    )
}
