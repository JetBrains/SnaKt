// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.postconditions

@AlwaysVerify
fun <!VIPER_TEXT!>maxValueControl<!>(): Int {
    postconditions<Int> { result -> result == 2147483647 }
    return 2147483646 + 1
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>maxValueOverflow<!>(): Int {
    postconditions<Int> { result -> result == -2147483648 }
    return 2147483647 + 1
}<!>

@AlwaysVerify
fun <!VIPER_TEXT!>maxValueNearBoundary<!>(): Int {
    postconditions<Int> { result -> result == 2147483647 }
    return 2147483647 + 0
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>minValueNegation<!>(): Int {
    postconditions<Int> { result -> result == -2147483648 }
    val value = -2147483648
    return -value
}<!>

@AlwaysVerify
fun <!VIPER_TEXT!>minValueNegationNearBoundary<!>(): Int {
    postconditions<Int> { result -> result == 2147483647 }
    return -(-2147483647)
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>minValueDivisionOverflow<!>(): Int {
    postconditions<Int> { result -> result == -2147483648 }
    return -2147483648 / -1
}<!>

@AlwaysVerify
fun <!VIPER_TEXT!>minValueDivisionControl<!>(): Int {
    postconditions<Int> { result -> result == -2147483648 }
    return -2147483648 / 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>minValueRemainder<!>(): Int {
    postconditions<Int> { result -> result == 0 }
    return -2147483648 % -1
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>minValueRemainderNearBoundary<!>(): Int {
    postconditions<Int> { result -> result == -1 }
    return -2147483648 % 2147483647
}<!>

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>negativeRemainderMinimal<!>(): Int {
    postconditions<Int> { result -> result == -2 }
    return -2 % 3
}<!>
