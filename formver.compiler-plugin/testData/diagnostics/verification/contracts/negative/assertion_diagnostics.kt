// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

@AlwaysVerify
fun <!VIPER_TEXT!>positiveNestedBoundary<!>(x: Int) {
    if (x >= 0) {
        if (x < 10) {
            verify(x + 1 > 0)
        }
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>failingNestedBoundary<!>(x: Int) {
    if (x >= 0) {
        if (x < 10) {
            verify(<!VIPER_VERIFICATION_ERROR!>x > 0<!>)
        }
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>multipleAssertionsAttributeTheFailure<!>(x: Int) {
    verify(
        x == x,
        <!VIPER_VERIFICATION_ERROR!>x != x<!>,
        x + 1 > x,
    )
}
