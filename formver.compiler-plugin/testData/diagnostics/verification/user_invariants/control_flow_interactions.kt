// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Pure
import org.jetbrains.kotlin.formver.plugin.loopInvariants
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions
import org.jetbrains.kotlin.formver.plugin.verify

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>next<!>(value: Int): Int {
    postconditions<Int> { result -> result == value + 1 }
    return value + 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>branchWithCall<!>(chooseFirst: Boolean): Int {
    val result = if (chooseFirst) next(0) else next(1)
    verify(result > 0)
    return result
}

@AlwaysVerify
fun <!VIPER_TEXT!>whenWithNestedCalls<!>(start: Int) {
    val result = next(next(start))
    when {
        start >= 0 -> verify(result >= 2)
        else -> verify(result == start + 2)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>loopCallWithEarlyReturn<!>(limit: Int): Int {
    preconditions { limit >= 0 }
    postconditions<Int> { result -> result == limit }

    var current = 0
    while (true) {
        loopInvariants {
            current >= 0
            current <= limit
        }
        if (current == limit) return current
        current = next(current)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>recursiveBranchWithCall<!>(remaining: Int): Int {
    preconditions { remaining >= 0 }
    postconditions<Int> { result -> result == remaining }

    return when (remaining) {
        0 -> 0
        else -> next(recursiveBranchWithCall(remaining - 1))
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>nestedCallBoundary<!>() {
    val result = next(next(0))
    verify(<!VIPER_VERIFICATION_ERROR!>result == 3<!>)
}
