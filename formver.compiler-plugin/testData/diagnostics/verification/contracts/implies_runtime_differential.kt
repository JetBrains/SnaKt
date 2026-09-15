// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.implies
import org.jetbrains.kotlin.formver.plugin.verify

@AlwaysVerify
fun <!VIPER_TEXT!>safeImplicationControl<!>() {
    verify(true implies (6 / 2 == 3))
}

@AlwaysVerify
fun <!VIPER_TEXT!>eagerImplicationRuntimeFailure<!>() {
    verify(false implies (<!DIVISION_BY_ZERO!>1 / 0<!> == 0))
}

@AlwaysVerify
fun <!VIPER_TEXT!>trueAntecedentDivisionBoundary<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>true implies (<!DIVISION_BY_ZERO!>1 / 0<!> == 0)<!>)
}
