// FULL_JDK
// FILE: same_file.kt

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>sameFileIncrement<!>(value: Int): Int {
    preconditions { value >= 0 }
    postconditions<Int> { result -> result == value + 1 }
    return value + 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>sameFileControl<!>(): Int {
    postconditions<Int> { result -> result == 1 }
    return sameFileIncrement(0)
}

// FILE: providers.kt

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>crossFileIncrement<!>(value: Int): Int {
    preconditions { value >= 0 }
    postconditions<Int> { result -> result == value + 1 }
    return value + 1
}

class Counter(private val offset: Int) {
    @AlwaysVerify
    fun <!VIPER_TEXT!>add<!>(value: Int): Int {
        preconditions { value >= 0 }
        postconditions<Int> { result -> result == value + offset }
        return value + offset
    }
}

// FILE: consumers.kt

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>crossFileFunctionControl<!>(): Int {
    postconditions<Int> { result -> result == 1 }
    return crossFileIncrement(0)
}

@AlwaysVerify
fun <!VIPER_TEXT!>crossFileClassControl<!>(): Int {
    postconditions<Int> { result -> result == 2 }
    return Counter(2).add(0)
}

@AlwaysVerify
fun <!VIPER_TEXT!>crossFileFunctionPreconditionViolation<!>() {
    <!VIPER_VERIFICATION_ERROR!>crossFileIncrement(-1)<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>crossFileClassPreconditionViolation<!>() {
    <!VIPER_VERIFICATION_ERROR!>Counter(2).add(-1)<!>
}
