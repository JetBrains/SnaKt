// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>quantifiedSuccessor<!>(seed: Int): Int {
    preconditions {
        seed >= 0
    }
    postconditions<Int> { result ->
        result == seed + 1
        forAll<Int> { candidate ->
            (candidate == seed) implies (result == candidate + 1)
        }
    }
    return seed + 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>usesQuantifiedContract<!>(seed: Int): Int {
    preconditions {
        seed >= 0
    }
    postconditions<Int> { result ->
        result > seed
    }
    val result = quantifiedSuccessor(seed)
    verify(result == seed + 1)
    return result
}

@AlwaysVerify
fun <!VIPER_TEXT!>violatesPropagatedPrecondition<!>(): Int {
    return <!VIPER_VERIFICATION_ERROR!>quantifiedSuccessor(-1)<!>
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>triggerlessExistentialBoundary<!>(seed: Int): Int {
    postconditions<Int> { result ->
        exists<Int> { witness ->
            witness == seed && result == witness
        }
    }
    return seed
}<!>

fun <!VIPER_TEXT!>impurePredicate<!>(value: Int): Boolean = value > 0

<!INTERNAL_ERROR!>@AlwaysVerify
fun impurePostcondition(value: Int) {
    postconditions<Unit> {
        impurePredicate(value)
    }
}<!>
