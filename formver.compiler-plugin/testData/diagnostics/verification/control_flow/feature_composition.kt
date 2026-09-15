// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Pure
import org.jetbrains.kotlin.formver.plugin.loopInvariants
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions
import org.jetbrains.kotlin.formver.plugin.verify

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>increment<!>(value: Int): Int {
    postconditions<Int> { result -> result == value + 1 }
    return value + 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>loopCallSeed<!>(limit: Int) {
    preconditions { limit >= 0 }

    var current = 0
    while (current < limit) {
        loopInvariants {
            0 <= current
            current <= limit
        }
        current = increment(current)
    }
    verify(current == limit)
}

@AlwaysVerify
fun <!VIPER_TEXT!>loopWhenCall<!>(limit: Int) {
    preconditions { limit >= 0 }

    var current = 0
    while (current < limit) {
        loopInvariants {
            0 <= current
            current <= limit
        }
        current = when {
            current + 1 < limit -> increment(current)
            else -> increment(current)
        }
    }
    verify(current == limit)
}

@AlwaysVerify
fun <!VIPER_TEXT!>loopWhenNestedCall<!>(limit: Int) {
    preconditions { limit >= 0 }

    var current = 0
    while (current < limit) {
        loopInvariants {
            0 <= current
            current <= limit
        }
        current = when {
            current + 1 < limit -> increment(increment(current))
            else -> increment(current)
        }
    }
    verify(current == limit)
}

@AlwaysVerify
fun <!VIPER_TEXT!>loopWhenNestedCallNegative<!>(limit: Int) {
    preconditions { limit >= 0 }

    var current = 0
    while (current < limit) {
        loopInvariants {
            0 <= current
            current <= limit
        }
        current = when {
            current + 1 < limit -> increment(increment(current))
            else -> increment(current)
        }
    }
    verify(<!VIPER_VERIFICATION_ERROR!>current == limit + 1<!>)
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>recursiveWhenCall<!>(value: Int): Int {
    preconditions { value >= 0 }
    postconditions<Int> { result -> result == value }

    return when (value) {
        0 -> 0
        else -> increment(recursiveWhenCall(value - 1))
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>loopWhenEarlyReturnCall<!>(limit: Int): Int {
    preconditions { limit >= 0 }
    postconditions<Int> { result -> result == limit }

    var current = 0
    while (current < limit) {
        loopInvariants {
            0 <= current
            current <= limit
        }
        when {
            current + 1 == limit -> return increment(current)
            else -> current = increment(current)
        }
    }
    return current
}
