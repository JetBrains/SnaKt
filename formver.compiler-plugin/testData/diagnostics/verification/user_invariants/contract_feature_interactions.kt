// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>successorWithImplications<!>(value: Int): Int {
    preconditions {
        value >= 0
    }
    postconditions<Int> { result ->
        (value == 0) implies (result == 1)
        (value > 0) implies (result > value)
    }
    return value + 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>useImplicationPostconditions<!>() {
    val result = successorWithImplications(0)
    verify(result == 1)
}

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>quantifiedIdentity<!>(value: Int): Int {
    postconditions<Int> { result ->
        result == value
        forAll<Int> {
            triggers(it == result)
            (it == result) implies (it == value)
        }
    }
    return value
}

@AlwaysVerify
fun <!VIPER_TEXT!>usePureQuantifiedPostcondition<!>(value: Int) {
    preconditions {
        value >= 0
    }
    val result = quantifiedIdentity(value)
    verify(result == value, result >= 0)
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>falsePostconditionControl<!>(value: Int): Int {
    postconditions<Int> { result ->
        result > value
    }
    return value
}<!>

<!PURITY_VIOLATION!>fun <!VERIFICATION_SKIPPED!>impureQuantifiedImplicationControl<!>(
    values: List<Int>,
    result: Int,
): Int {
    postconditions<Int> {
        forAll<Int> { index ->
            (index == 0) implies (values[index] == result)
        }
    }
    return result
}<!>
