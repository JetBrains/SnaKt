// FULL_JDK
// REPLACE_STDLIB_EXTENSIONS

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Pure
import org.jetbrains.kotlin.formver.plugin.loopInvariants
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions
import org.jetbrains.kotlin.formver.plugin.verify

@Pure
fun <!VIPER_TEXT!>incrementForFailurePropagation<!>(x: Int): Int {
    postconditions<Int> { result -> result == x + 1 }
    return x + 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>nestedCallsPreserveEvaluationOrder<!>() {
    val result = incrementForFailurePropagation(incrementForFailurePropagation(0))
    verify(result == 2)
}

@AlwaysVerify
fun <!VIPER_TEXT!>nestedCallFailureRemainsVisible<!>() {
    val result = incrementForFailurePropagation(incrementForFailurePropagation(0))
    verify(<!VIPER_VERIFICATION_ERROR!>result == 1<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>branchLoopAndEarlyReturn<!>(limit: Int): Int {
    preconditions { limit >= 0 }
    postconditions<Int> { result -> result >= 0 }

    var current = 0
    while (current < limit) {
        loopInvariants {
            current >= 0
            current <= limit
        }
        current = incrementForFailurePropagation(current)
    }

    return when (current) {
        0 -> 0
        else -> current
    }
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>earlyReturnFailureRemainsVisible<!>(failEarly: Boolean): Int {
    postconditions<Int> { result -> result >= 0 }

    if (failEarly) {
        return -1
    }
    return 0
}<!>

@AlwaysVerify
fun <!VIPER_TEXT!>boundedRecursiveCalls<!>(remaining: Int) {
    preconditions { remaining >= 0 }

    if (remaining == 0) {
        verify(remaining == 0)
        return
    }
    boundedRecursiveCalls(remaining - 1)
}

@AlwaysVerify
fun <!VIPER_TEXT!>recursiveBaseFailureRemainsVisible<!>(remaining: Int) {
    preconditions { remaining >= 0 }

    if (remaining == 0) {
        verify(<!VIPER_VERIFICATION_ERROR!>remaining > 0<!>)
        return
    }
    recursiveBaseFailureRemainsVisible(remaining - 1)
}
