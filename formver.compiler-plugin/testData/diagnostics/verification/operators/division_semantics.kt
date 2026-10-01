// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions

@AlwaysVerify
fun <!VIPER_TEXT!>positiveOperandsDivision<!>(): Int {
    postconditions<Int> { result -> result == 3 }
    return 7 / 2
}

@AlwaysVerify
fun <!VIPER_TEXT!>negativeDividendDivision<!>(): Int {
    postconditions<Int> { result -> result == -3 }
    return -7 / 2
}

@AlwaysVerify
fun <!VIPER_TEXT!>negativeDivisorDivision<!>(): Int {
    postconditions<Int> { result -> result == -3 }
    return 7 / -2
}

@AlwaysVerify
fun <!VIPER_TEXT!>bothOperandsNegativeDivision<!>(): Int {
    postconditions<Int> { result -> result == 3 }
    return -7 / -2
}

@AlwaysVerify
fun <!VIPER_TEXT!>zeroDividendDivision<!>(): Int {
    postconditions<Int> { result -> result == 0 }
    return 0 / -2
}

@AlwaysVerify
fun <!VIPER_TEXT!>divisionRemainderIdentity<!>(a: Int, b: Int): Int {
    preconditions { b != 0 }
    postconditions<Int> { result -> result == a }
    return (a / b) * b + a % b
}

// Int overflow is not modelled, so this is 2^31 in Viper rather than Int.MIN_VALUE.
@AlwaysVerify
fun <!VIPER_TEXT!>minimumValueNegativeOneDivision<!>(): Int {
    postconditions<Int> { result -> result == -(-2147483647 - 1) }
    return (-2147483647 - 1) / -1
}
