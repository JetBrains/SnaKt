// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>splitContract<!>(value: Int): Int {
    preconditions {
        value >= 0
        value <= 10
    }
    postconditions<Int> { result ->
        result >= 0
        result <= 10
    }
    verify(value >= 0, value <= 10)
    return value
}

@AlwaysVerify
fun <!VIPER_TEXT!>conjoinedContract<!>(value: Int): Int {
    preconditions {
        value >= 0 && value <= 10
    }
    postconditions<Int> { renamedResult ->
        renamedResult >= 0 && renamedResult <= 10
    }
    verify(value >= 0 && value <= 10)
    return value
}

@AlwaysVerify
fun <!VIPER_TEXT!>propagatedEquivalentContracts<!>(value: Int) {
    preconditions { value >= 0 && value <= 10 }
    val splitResult = splitContract(value)
    val conjoinedResult = conjoinedContract(value)
    verify(splitResult >= 0, splitResult <= 10)
    verify(conjoinedResult >= 0 && conjoinedResult <= 10)
}

@AlwaysVerify
fun <!VIPER_TEXT!>quantifiedImplication<!>() {
    verify(
        forAll<Int> { binder ->
            (binder == 0) implies (binder * binder == 0)
        }
    )
}

@AlwaysVerify
fun <!VIPER_TEXT!>quantifiedDisjunction<!>() {
    verify(
        forAll<Int> { renamedBinder ->
            renamedBinder != 0 || renamedBinder * renamedBinder == 0
        }
    )
}

@AlwaysVerify
fun <!VIPER_TEXT!>failingImplication<!>(value: Int) {
    preconditions { value > 0 }
    verify(<!VIPER_VERIFICATION_ERROR!>(value > 0) implies (value < 0)<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>failingDisjunction<!>(value: Int) {
    preconditions { value > 0 }
    verify(<!VIPER_VERIFICATION_ERROR!>value <= 0 || value < 0<!>)
}

@NeverVerify
fun <!VERIFICATION_SKIPPED!>impureEquivalentForms<!>() {
    var value = 0
    verify(
        <!PURITY_VIOLATION!>value++ == 0<!>,
        <!PURITY_VIOLATION!>++value == 1<!>,
    )
}
