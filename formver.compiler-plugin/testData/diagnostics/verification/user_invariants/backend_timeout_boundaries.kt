// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// A small recursive obligation with a quantified postcondition. This is the
// positive control: it must finish and verify rather than being mistaken for
// an unreported backend timeout.
@AlwaysVerify
fun <!VIPER_TEXT!>recursiveQuantifiedSuccess<!>(n: Int): Int {
    preconditions { n >= 0 }
    postconditions<Int> { res ->
        res == 0
        forAll<Int> { it == it }
    }

    return if (n == 0) 0 else recursiveQuantifiedSuccess(n - 1)
}

// The same bounded recursive shape with a false universal obligation. This is
// the negative control: it must produce a proof failure, not silent success.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>recursiveQuantifiedFailure<!>(n: Int): Int {
    preconditions { n >= 0 }
    postconditions<Int> { res ->
        res == 0
        forAll<Int> { it > 0 }
    }

    return if (n == 0) 0 else recursiveQuantifiedFailure(n - 1)
}<!>
