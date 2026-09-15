// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>verifiedControl<!>(x: Int) {
    preconditions { x >= 0 }
    verify(x >= 0)
}

@AlwaysVerify
fun <!VIPER_TEXT!>proofFailureControl<!>(x: Int) {
    preconditions { x >= 0 }
    verify(<!VIPER_VERIFICATION_ERROR!>x > 0<!>)
}

@AlwaysVerify
fun unsupportedConversionControl(): Double = <!INTERNAL_ERROR!>1.0<!>
