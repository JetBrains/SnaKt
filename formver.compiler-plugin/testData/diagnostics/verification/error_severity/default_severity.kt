// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// Pins the default severity: a failed proof is reported as a warning.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>unprovablePostcondition<!>(x: Int): Int {
    postconditions<Int> { result -> result > x }
    return x
}<!>
