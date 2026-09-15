// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.postconditions

@AlwaysVerify
fun <!VIPER_TEXT!>negativeDividendRemainder<!>(): Int {
    postconditions<Int> { result -> result == -2 }
    return -2 % 3
}

@AlwaysVerify
fun <!VIPER_TEXT!>negativeDivisorRemainder<!>(): Int {
    postconditions<Int> { result -> result == 2 }
    return 2 % -3
}

@AlwaysVerify
fun <!VIPER_TEXT!>bothOperandsNegativeRemainder<!>(): Int {
    postconditions<Int> { result -> result == -2 }
    return -2 % -3
}

@AlwaysVerify
fun <!VIPER_TEXT!>minimumValueRemainder<!>(): Int {
    postconditions<Int> { result -> result == -1 }
    return (-2147483647 - 1) % 2147483647
}

@AlwaysVerify
fun <!VIPER_TEXT!>minimumValueNegativeOneRemainder<!>(): Int {
    postconditions<Int> { result -> result == 0 }
    return (-2147483647 - 1) % -1
}
