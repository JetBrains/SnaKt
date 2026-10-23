// FULL_JDK
// DEFAULT_SELECTION

import org.jetbrains.kotlin.formver.plugin.*

// A specification block alone makes these two functions targets.

<!VIPER_VERIFICATION_ERROR!>fun <!VIPER_TEXT!>alwaysPositive<!>(x: Int): Int {
    postconditions<Int> {
        it > 0
    }
    return x
}<!>

fun <!VIPER_TEXT!>atLeastTen<!>(x: Int): Int {
    preconditions {
        x >= 10
    }
    verify(<!VIPER_VERIFICATION_ERROR!>x > 100<!>)
    return x
}

@AlwaysVerify
fun <!VIPER_TEXT!>caller<!>(): Int {
    val positive = alwaysPositive(-1)
    verify(positive > 0)
    return atLeastTen(20)
}

// Neither specified nor annotated: outside default target selection.
fun unspecified(x: Int): Int = x + 1

// A nested lambda owns its blocks; it must not select this enclosing function.
fun lambdaOnly() = { preconditions { true } }

// Matching spelling alone is not a SnaKt specification.
object OtherSpecs {
    fun preconditions() = Unit
}

fun sameNameNonDsl() {
    OtherSpecs.preconditions()
    verify(false)
}

// Only misplaced blocks: still a target, so the blocks are reported.
fun <!VERIFICATION_SKIPPED!>onlyMisplacedPostconditions<!>(x: Int): Int {
    val y = x
    <!IGNORED_SPEC_BLOCK!>postconditions<Int> { it > 0 }<!>
    return y
}

fun <!VERIFICATION_SKIPPED!>onlyMisplacedLoopInvariants<!>(n: Int) {
    var i = 0
    while (i < n) {
        i = i + 1
        <!IGNORED_SPEC_BLOCK!>loopInvariants { i >= 0 }<!>
    }
}
