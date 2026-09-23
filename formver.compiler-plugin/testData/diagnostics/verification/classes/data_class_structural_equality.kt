// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

data class EqualityPair(val first: Int, val second: Int)

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>equalValues<!>(): Boolean {
    postconditions<Boolean> { result -> result }
    return EqualityPair(10, 20) == EqualityPair(10, 20)
}<!>

@AlwaysVerify
fun <!VIPER_TEXT!>differentValues<!>(): Boolean {
    postconditions<Boolean> { result -> !result }
    return EqualityPair(10, 20) == EqualityPair(10, 30)
}

@AlwaysVerify
fun <!VIPER_TEXT!>sameReference<!>(pair: EqualityPair): Boolean {
    postconditions<Boolean> { result -> result }
    return pair == pair
}
