// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>independentDeclarationsLeftFirst<!>() {
    val left = 20 + 1
    val right = 6 * 7

    verify(left == 21 && right == 42)
}

@AlwaysVerify
fun <!VIPER_TEXT!>independentDeclarationsRightFirst<!>() {
    val right = 6 * 7
    val left = 20 + 1

    verify(left == 21 && right == 42)
}

@AlwaysVerify
fun <!VIPER_TEXT!>independentAssignmentsLeftFirst<!>() {
    var left = 10
    var right = 20

    left = left + 1
    right = right + 2

    verify(left == 11 && right == 22)
}

@AlwaysVerify
fun <!VIPER_TEXT!>independentAssignmentsRightFirst<!>() {
    var left = 10
    var right = 20

    right = right + 2
    left = left + 1

    verify(left == 11 && right == 22)
}

@AlwaysVerify
fun <!VIPER_TEXT!>failingIndependentAssignmentsLeftFirst<!>() {
    var left = 10
    var right = 20

    left = left + 1
    right = right + 2

    verify(<!VIPER_VERIFICATION_ERROR!>left == 12 && right == 22<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>failingIndependentAssignmentsRightFirst<!>() {
    var left = 10
    var right = 20

    right = right + 2
    left = left + 1

    verify(<!VIPER_VERIFICATION_ERROR!>left == 12 && right == 22<!>)
}
