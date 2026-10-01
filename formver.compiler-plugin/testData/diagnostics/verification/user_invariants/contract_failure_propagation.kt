// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>nonNegativeIdentity<!>(x: Int): Int {
    preconditions { x >= 0 }
    postconditions<Int> { result ->
        result >= 0
        forAll<Int> { candidate ->
            (candidate == x) implies (result == candidate)
        }
    }
    return x
}

@AlwaysVerify
fun <!VIPER_TEXT!>contractPositiveControl<!>() {
    val result = nonNegativeIdentity(0)
    verify(result == 0)
    verify(forAll<Int> { value -> (value == 0) implies (value >= 0) })
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>brokenPostcondition<!>(x: Int): Int {
    preconditions { x >= 0 }
    postconditions<Int> { result -> result >= x }
    return x - 1
}<!>

@AlwaysVerify
fun <!VIPER_TEXT!>violatesPropagatedPrecondition<!>() {
    val result = <!VIPER_VERIFICATION_ERROR!>nonNegativeIdentity(-1)<!>
    verify(result >= 0)
}

@AlwaysVerify
fun <!VIPER_TEXT!>failingQuantifiedVerify<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>forAll<Int> { value -> value != 0 }<!>)
}

fun <!VIPER_TEXT!>impurePredicate<!>(): Boolean = true

<!INTERNAL_ERROR!>@AlwaysVerify
fun impurePostcondition(): Boolean {
    postconditions<Boolean> { impurePredicate() }
    return true
}<!>
