// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

@AlwaysVerify
fun <!VIPER_TEXT!>repeatedPositiveControl<!>() {
    verify(true)
    verify(1 + 1 == 2)
}

@AlwaysVerify
fun <!VIPER_TEXT!>perturbedPositiveControl<!>() {
    val harmless = 0
    verify(true)
    verify(harmless + 2 == 2)
}

@AlwaysVerify
fun <!VIPER_TEXT!>repeatedNegativeControl<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
    verify(1 + 1 == 3)
}

@AlwaysVerify
fun <!VIPER_TEXT!>perturbedNegativeControl<!>() {
    val harmless = 0
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
    verify(harmless + 2 == 3)
}
