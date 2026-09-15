// FULL_JDK
// REPLACE_STDLIB_EXTENSIONS

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.loopInvariants
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions
import org.jetbrains.kotlin.formver.plugin.verify

fun <!VIPER_TEXT!>signAtBoundary<!>(value: Int): Int {
    postconditions<Int> { result ->
        (value < 0 && result == -1) ||
            (value == 0 && result == 0) ||
            (value > 0 && result == 1)
    }
    return when {
        value < 0 -> -1
        value == 0 -> 0
        else -> 1
    }
}

fun <!VIPER_TEXT!>countToBoundary<!>(limit: Int): Int {
    preconditions { limit >= 0 }
    postconditions<Int> { result -> result == limit }
    var current = 0
    while (current < limit) {
        loopInvariants {
            current >= 0
            current <= limit
        }
        current = current + 1
    }
    return current
}

fun <!VIPER_TEXT!>recursiveToBoundary<!>(value: Int): Int {
    preconditions { value >= 0 }
    postconditions<Int> { result -> result == 0 }
    if (value == 0) return value
    return recursiveToBoundary(value - 1)
}

@AlwaysVerify
fun <!VIPER_TEXT!>boundaryControls<!>() {
    val below = signAtBoundary(-1)
    val at = signAtBoundary(0)
    val above = signAtBoundary(1)
    val minimum = signAtBoundary(-2147483647 - 1)
    val maximum = signAtBoundary(2147483647)
    verify(below == -1)
    verify(at == 0)
    verify(above == 1)
    verify(minimum == -1)
    verify(maximum == 1)

    val empty = countToBoundary(0)
    val singleton = countToBoundary(1)
    val multiple = countToBoundary(2)
    verify(empty == 0)
    verify(singleton == 1)
    verify(multiple == 2)

    val base = recursiveToBoundary(0)
    val oneStep = recursiveToBoundary(1)
    val twoSteps = recursiveToBoundary(2)
    verify(base == 0)
    verify(oneStep == 0)
    verify(twoSteps == 0)
}
