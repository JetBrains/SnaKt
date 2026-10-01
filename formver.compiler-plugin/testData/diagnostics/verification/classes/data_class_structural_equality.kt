// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

data class EqualityPair(val first: Int, val second: Int)

// Known gap: `==` on data classes is lowered to reference identity.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>equalValues<!>(): Boolean {
    postconditions<Boolean> { result -> result }
    return EqualityPair(10, 20) == EqualityPair(10, 20)
}<!>

// Passes only because two fresh objects are distinct references, not by comparing fields.
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
