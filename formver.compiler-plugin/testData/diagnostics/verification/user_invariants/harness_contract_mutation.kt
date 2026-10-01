// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.postconditions

@AlwaysVerify
fun <!VIPER_TEXT!>preservesExactResult<!>(value: Int): Int {
    postconditions<Int> { result ->
        result == value
    }
    return value
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>reversesExactResult<!>(value: Int): Int {
    postconditions<Int> { result ->
        result == value + 1
    }
    return value
}<!>
