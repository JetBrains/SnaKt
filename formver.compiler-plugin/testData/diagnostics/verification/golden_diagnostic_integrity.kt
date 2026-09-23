// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>unconstrainedAssertion<!>(x: Int) {
    verify(<!VIPER_VERIFICATION_ERROR!>x >= 0<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>assertionWithExactPrecondition<!>(x: Int) {
    preconditions { x >= 0 }
    verify(x >= 0)
}

@AlwaysVerify
fun <!VIPER_TEXT!>assertionAtStrictBoundary<!>(x: Int) {
    preconditions { x >= 0 }
    verify(<!VIPER_VERIFICATION_ERROR!>x > 0<!>)
}
