// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>contractStepUp<!>(value: Int): Int {
    preconditions {
        value >= 0
        value < 2
    }
    postconditions<Int> { result ->
        result == value + 1
        result > value
    }
    return value + 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>contractStepDown<!>(value: Int): Int {
    preconditions {
        value > 0
        value <= 2
    }
    postconditions<Int> { result ->
        result == value - 1
        result < value
    }
    return value - 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>quantifiedEcho<!>(value: Int): Int {
    preconditions {
        forAll<Int> { candidate ->
            (candidate == value) implies (candidate >= 0)
        }
    }
    postconditions<Int> { result ->
        result == value
        forAll<Int> { candidate ->
            (candidate == result) implies (candidate == value)
        }
    }
    return value
}

@AlwaysVerify
fun <!VIPER_TEXT!>roundTripSequence<!>() {
    var state = 0
    verify(state == 0)

    state = contractStepUp(state)
    verify(state == 1, state > 0)

    state = contractStepUp(state)
    verify(state == 2, state > 1)

    state = contractStepDown(state)
    verify(state == 1, state < 2)

    state = contractStepDown(state)
    verify(state == 0, state >= 0)

    state = quantifiedEcho(state)
    verify(state == 0)
}

@AlwaysVerify
fun <!VIPER_TEXT!>shortRoundTripControl<!>() {
    val advanced = contractStepUp(0)
    verify(advanced == 1)
    val restored = contractStepDown(advanced)
    verify(restored == 0)
}

@AlwaysVerify
fun <!VIPER_TEXT!>upperBoundaryViolation<!>() {
    <!VIPER_VERIFICATION_ERROR!>contractStepUp(2)<!>
}

@NeverVerify
fun <!VERIFICATION_SKIPPED!>impureVerifySequence<!>() {
    var state = 0
    verify(state == 0)
    verify(<!PURITY_VIOLATION!>state++ == 0<!>)
    verify(state == 0)
}
