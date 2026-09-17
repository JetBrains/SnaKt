// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// Independent oracle for implication over the complete Boolean domain:
//
//   a      b      a implies b
//   false  false  true
//   false  true   true
//   true   false  false
//   true   true   true

@Pure
fun <!VIPER_TEXT!>modeledImplies<!>(a: Boolean, b: Boolean): Boolean {
    postconditions<Boolean> { result ->
        result == (!a || b)
    }
    return !a || b
}

@AlwaysVerify
fun <!VIPER_TEXT!>completeTruthTable<!>() {
    verify(modeledImplies(false, false))
    verify(modeledImplies(false, true))
    verify(!modeledImplies(true, false))
    verify(modeledImplies(true, true))
}

@AlwaysVerify
fun <!VIPER_TEXT!>quantifiedOracle<!>() {
    verify(
        forAll<Boolean> { a ->
            forAll<Boolean> { b ->
                modeledImplies(a, b) == (a implies b)
            }
        }
    )
}

@AlwaysVerify
fun <!VIPER_TEXT!>propagateRestrictedContract<!>(a: Boolean, b: Boolean): Boolean {
    preconditions {
        a || b
    }
    postconditions<Boolean> { result ->
        result == b
    }
    return modeledImplies(a, b)
}

@AlwaysVerify
fun <!VIPER_TEXT!>restrictedContractCaller<!>(b: Boolean) {
    val result = propagateRestrictedContract(true, b)
    verify(result == b)
}

@AlwaysVerify
fun <!VIPER_TEXT!>negativeTruthTableControl<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>modeledImplies(true, false)<!>)
}

fun <!VIPER_TEXT!>impurePredicate<!>(value: Int): Boolean = value >= 0

<!INTERNAL_ERROR!>@AlwaysVerify
fun impurePreconditionControl(initial: Int) {
    preconditions {
        impurePredicate(initial)
    }
}<!>
