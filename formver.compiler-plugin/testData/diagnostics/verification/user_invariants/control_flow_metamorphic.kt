// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>increment<!>(value: Int): Int {
    postconditions<Int> { result -> result == value + 1 }
    return value + 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>recursiveEarlyReturn<!>(remaining: Int): Int {
    preconditions { remaining >= 0 }
    postconditions<Int> { result -> result == 0 }

    if (remaining == 0) return 0
    return recursiveEarlyReturn(remaining - 1)
}

@AlwaysVerify
fun <!VIPER_TEXT!>recursiveIfExpression<!>(count: Int): Int {
    preconditions { count >= 0 }
    postconditions<Int> { result -> result == 0 }

    return if (count == 0) 0 else recursiveIfExpression(count - 1)
}

@AlwaysVerify
fun <!VIPER_TEXT!>loopCountdownGreaterThan<!>(remaining: Int): Int {
    preconditions { remaining >= 0 }
    postconditions<Int> { result -> result == 0 }

    var count = remaining
    while (count > 0) {
        loopInvariants { count >= 0 }
        count--
    }
    return count
}

@AlwaysVerify
fun <!VIPER_TEXT!>loopCountdownNotZero<!>(initial: Int): Int {
    preconditions { initial >= 0 }
    postconditions<Int> { answer -> answer == 0 }

    var cursor = initial
    while (cursor != 0) {
        loopInvariants { cursor >= 0 }
        cursor = cursor - 1
    }
    return cursor
}

@AlwaysVerify
fun <!VIPER_TEXT!>ifClassification<!>(value: Int): Int {
    postconditions<Int> { result ->
        (value == 0 && result == 0) || (value != 0 && result == 1)
    }
    return if (value == 0) 0 else 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>whenClassification<!>(renamed: Int): Int {
    postconditions<Int> { answer ->
        (renamed == 0 && answer == 0) || (renamed != 0 && answer == 1)
    }
    return when (renamed) {
        0 -> 0
        else -> 1
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>nestedCalls<!>(value: Int): Int {
    postconditions<Int> { result -> result == value + 2 }
    return increment(increment(value))
}

@AlwaysVerify
fun <!VIPER_TEXT!>sequentialCalls<!>(renamed: Int): Int {
    postconditions<Int> { answer -> answer == renamed + 2 }
    val first = increment(renamed)
    return increment(first)
}

@AlwaysVerify
fun <!VIPER_TEXT!>independentOrderLeftFirst<!>(left: Int, right: Int): Int {
    postconditions<Int> { result -> result == left + right + 2 }
    val leftIncremented = increment(left)
    val rightIncremented = increment(right)
    return leftIncremented + rightIncremented
}

@AlwaysVerify
fun <!VIPER_TEXT!>independentOrderRightFirst<!>(first: Int, second: Int): Int {
    postconditions<Int> { answer -> answer == first + second + 2 }
    val secondIncremented = increment(second)
    val firstIncremented = increment(first)
    return firstIncremented + secondIncremented
}

@NeverVerify
fun <!VIPER_TEXT!>dependentCallOrderBoundary<!>(value: Int): Int {
    postconditions<Int> { result -> result == value + 2 }
    val incremented = increment(value)
    return increment(value - incremented)
}
