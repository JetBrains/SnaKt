// FULL_JDK
// FAIL_ON_VERIFICATION_ERROR

import org.jetbrains.kotlin.formver.plugin.*

// Pins FAIL_ON_VERIFICATION_ERROR: a failed proof is reported as an error.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>unprovablePostcondition<!>(x: Int): Int {
    postconditions<Int> { result -> result > x }
    return x
}<!>
