import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>positiveProof<!>(x: Int) {
    verify(x == x)
}

@AlwaysVerify
fun <!VIPER_TEXT!>negativeProof<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@NeverVerify
fun <!VIPER_TEXT!>skippedNegativeProof<!>() {
    verify(false)
}

@AlwaysVerify
fun <!VERIFICATION_SKIPPED!>conversionDiagnosticSuppressesVerification<!>() {
    var x = 0
    verify(<!PURITY_VIOLATION!>x++ == 0<!>)
}

@Pure
fun <!VIPER_TEXT!>recursiveSpecification<!>(n: Int): Int {
    preconditions {
        n >= 0
    }
    postconditions<Int> { result ->
        (n == 0) implies (result == 0)
        (n > 0) implies (result == <!CONSISTENCY!>recursiveSpecification(n - 1)<!> + 1)
    }
    return if (n == 0) 0 else recursiveSpecification(n - 1) + 1
}

// FULL_JDK
