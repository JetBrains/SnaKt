// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Pure
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions
import org.jetbrains.kotlin.formver.plugin.verify

@Pure
fun <!VIPER_TEXT!>decimalPair<!>(left: Int, right: Int): Int {
    postconditions<Int> { result -> result == left * 10 + right }
    return left * 10 + right
}

@AlwaysVerify
fun <!VIPER_TEXT!>whenSubjectEvaluatedOnce<!>() {
    var subject = 1
    val result = when (subject++) {
        1 -> 10
        else -> 20
    }

    verify(result == 10)
    verify(subject == 2)
}

@AlwaysVerify
fun <!VIPER_TEXT!>callArgumentsEvaluateLeftToRight<!>() {
    var next = 1
    val result = decimalPair(next++, next++)

    verify(result == 12)
    verify(next == 3)
}

@AlwaysVerify
fun <!VIPER_TEXT!>shortCircuitSkipsUnsafeCall<!>() {
    val divisor = 0
    val result = divisor != 0 && 100 / divisor > 1

    verify(!result)
}

@Pure
fun <!VIPER_TEXT!>boundedFactorial<!>(n: Int): Int {
    preconditions { n >= 0 }
    postconditions<Int> { result -> result >= 1 }
    return if (n == 0) 1 else n * boundedFactorial(n - 1)
}

@AlwaysVerify
fun <!VIPER_TEXT!>nestedRecursiveCallControl<!>() {
    val result = decimalPair(boundedFactorial(3), boundedFactorial(2))
    verify(<!VIPER_VERIFICATION_ERROR!>result == 62<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>negativeEvaluationOrderControl<!>() {
    var next = 1
    val result = decimalPair(next++, next++)

    verify(<!VIPER_VERIFICATION_ERROR!>result == 21<!>)
}
