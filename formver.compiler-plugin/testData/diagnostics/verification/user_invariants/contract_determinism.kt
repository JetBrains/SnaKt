// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>deterministicContractA<!>(value: Int): Int {
    preconditions {
        value >= 0
    }
    postconditions<Int> { result ->
        result == value + 1
        (value >= 0) implies (result > 0)
        forAll<Int> { candidate -> candidate == candidate }
    }
    verify(value >= 0)
    return value + 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>deterministicContractB<!>(value: Int): Int {
    preconditions {
        (value >= 0)
    }
    postconditions<Int> { result ->
        (result == value + 1)
        (value >= 0) implies ((result > 0))
        forAll<Int> { candidate -> (candidate == candidate) }
    }
    verify((value >= 0))
    return (value + 1)
}

@AlwaysVerify
fun <!VIPER_TEXT!>contractsPropagateDeterministically<!>(value: Int) {
    preconditions { value >= 0 }
    val first = deterministicContractA(value)
    val second = deterministicContractB(value)
    verify(first == second, first > 0, second > 0)
}

@AlwaysVerify
fun <!VIPER_TEXT!>expectedFailureA<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>expectedFailureB<!>() {
    verify((<!VIPER_VERIFICATION_ERROR!>false<!>))
}
