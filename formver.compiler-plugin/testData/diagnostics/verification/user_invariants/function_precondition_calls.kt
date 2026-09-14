// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions

@AlwaysVerify
fun <!VIPER_TEXT!>requirePositive<!>(value: Int): Int {
    preconditions { value > 0 }
    postconditions<Int> { it == value }
    return value
}

@AlwaysVerify
fun <!VIPER_TEXT!>directSatisfied<!>() {
    requirePositive(1)
}

@AlwaysVerify
fun <!VIPER_TEXT!>directViolated<!>() {
    <!VIPER_VERIFICATION_ERROR!>requirePositive(0)<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>nestedSatisfied<!>() {
    requirePositive(requirePositive(1))
}

@AlwaysVerify
fun <!VIPER_TEXT!>nestedViolated<!>() {
    requirePositive(<!VIPER_VERIFICATION_ERROR!>requirePositive(0)<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>expressionArgumentSatisfied<!>() {
    requirePositive(2 - 1)
}

@AlwaysVerify
fun <!VIPER_TEXT!>expressionArgumentViolated<!>() {
    <!VIPER_VERIFICATION_ERROR!>requirePositive(1 - 1)<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>branchSatisfied<!>(chooseFirst: Boolean) {
    if (chooseFirst) {
        requirePositive(1)
    } else {
        requirePositive(2)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>branchViolated<!>(chooseFirst: Boolean) {
    if (chooseFirst) {
        requirePositive(1)
    } else {
        <!VIPER_VERIFICATION_ERROR!>requirePositive(0)<!>
    }
}
