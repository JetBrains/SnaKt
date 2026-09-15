// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// Independent bounded oracle for x in -2..2:
//
//   x                 -2  -1   0   1   2
//   triangular(|x|)    3   1   0   1   3
//   score(x)           -3  -1   7  11  23

@Pure
fun <!VIPER_TEXT!>recursiveTriangular<!>(n: Int): Int {
    preconditions { 0 <= n && n <= 2 }
    postconditions<Int> { result -> result == n * (n + 1) / 2 }

    if (n == 0) return 0
    return n + recursiveTriangular(n - 1)
}

@Pure
fun <!VIPER_TEXT!>recursiveScore<!>(x: Int): Int {
    preconditions { -2 <= x && x <= 2 }
    postconditions<Int> { result ->
        (x < 0) implies (result == -(x * (x - 1) / 2))
        (x == 0) implies (result == 7)
        (x == 1) implies (result == 11)
        (x == 2) implies (result == 23)
    }

    return if (x < 0) {
        -recursiveTriangular(-x)
    } else {
        val triangular = recursiveTriangular(x)
        when (x) {
            0 -> 7
            1 -> triangular + 10
            else -> triangular + 20
        }
    }
}

// Boundary probe: Kotlin never calls recursiveTriangular for x == -1. The
// generated pure-function expression currently hoists the call before the
// early-return condition, producing an unreachable precondition failure.
@Pure
fun <!VIPER_TEXT!>earlyReturnCallBoundary<!>(x: Int): Int {
    preconditions { -1 <= x && x <= 0 }
    if (x < 0) return 0
    val triangular = <!VIPER_VERIFICATION_ERROR!>recursiveTriangular(x)<!>
    return triangular
}

@AlwaysVerify
fun <!VIPER_TEXT!>loopScore<!>(x: Int): Int {
    preconditions { -2 <= x && x <= 2 }
    postconditions<Int> { result ->
        (x < 0) implies (result == -(x * (x - 1) / 2))
        (x == 0) implies (result == 7)
        (x == 1) implies (result == 11)
        (x == 2) implies (result == 23)
    }

    val magnitude = if (x < 0) -x else x
    var i = 0
    var triangular = 0
    while (i < magnitude) {
        loopInvariants {
            0 <= i && i <= magnitude
            triangular == i * (i + 1) / 2
        }
        i += 1
        triangular += i
    }

    if (x < 0) return -triangular
    return when (x) {
        0 -> 7
        1 -> triangular + 10
        else -> triangular + 20
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>boundedOraclePositiveControls<!>() {
    verify(recursiveScore(-2) == -3)
    verify(recursiveScore(-1) == -1)
    verify(recursiveScore(0) == 7)
    verify(recursiveScore(1) == 11)
    verify(recursiveScore(2) == 23)

    // Evaluate the impure method calls in source order, then compare their
    // results with the independently specified recursive model.
    val loopMinusTwo = loopScore(-2)
    val loopMinusOne = loopScore(-1)
    val loopZero = loopScore(0)
    val loopOne = loopScore(1)
    val loopTwo = loopScore(2)
    verify(loopMinusTwo == recursiveScore(-2))
    verify(loopMinusOne == recursiveScore(-1))
    verify(loopZero == recursiveScore(0))
    verify(loopOne == recursiveScore(1))
    verify(loopTwo == recursiveScore(2))
}

@AlwaysVerify
fun <!VIPER_TEXT!>boundedOracleNegativeControl<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>recursiveScore(2) == 22<!>)
}
