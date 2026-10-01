// FULL_JDK

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import org.jetbrains.kotlin.formver.plugin.*

@Pure
fun <!VIPER_TEXT!>boundedIdentity<!>(value: Int): Int {
    preconditions {
        value >= -1
        value <= 1
    }
    postconditions<Int> { result ->
        result == value
        result >= -1
        result <= 1
    }
    return value
}

@AlwaysVerify
fun <!VIPER_TEXT!>inclusiveBoundaryControls<!>() {
    verify(boundedIdentity(-1) == -1)
    verify(boundedIdentity(0) == 0)
    verify(boundedIdentity(1) == 1)
}

@AlwaysVerify
fun <!VIPER_TEXT!>immediatelyBelowPrecondition<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>boundedIdentity(-2)<!> == -2)
}

@AlwaysVerify
fun <!VIPER_TEXT!>vacuousQuantifierBoundary<!>() {
    verify(forAll<Int> { i -> (0 <= i && i < 0) implies false })
}

@AlwaysVerify
fun <!VIPER_TEXT!>singletonQuantifierBoundary<!>() {
    verify(forAll<Int> { i -> (0 <= i && i < 1) implies (i == 0) })
}

@AlwaysVerify
fun <!VIPER_TEXT!>immediatelyAboveSingletonBoundary<!>() {
    verify(
        <!VIPER_VERIFICATION_ERROR!>forAll<Int> { i ->
            (0 <= i && i < 2) implies (i == 0)
        }<!>
    )
}

@Pure
fun <!VIPER_TEXT!>pureZero<!>(): Int = 0

@AlwaysVerify
fun <!VIPER_TEXT!>pureSpecificationExpression<!>() {
    verify(pureZero() == 0)
}

fun <!VERIFICATION_SKIPPED!>impureSpecificationExpression<!>() {
    var value = 0
    verify(<!PURITY_VIOLATION!>value++ == 0<!>)
}

@Pure
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>isPresent<!>(value: String?): Boolean {
    contract {
        returns(true) implies (value != null)
    }
    return value != null
}

@AlwaysVerify
fun <!VIPER_TEXT!>contractPropagationBoundaries<!>() {
    val empty = ""
    verify(isPresent(empty))

    val singleton = "x"
    verify(isPresent(singleton))

    val absent: String? = null
    verify(!isPresent(absent))
}
