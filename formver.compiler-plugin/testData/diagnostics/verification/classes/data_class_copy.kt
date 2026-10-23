// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

data class CopyPair(val first: Int, val second: Int)

// Known gap: `copy` is called with fewer arguments than it declares (defaults are dropped), and it verifies without any result semantics.
@AlwaysVerify
fun <!VIPER_TEXT!>copyOneProperty<!>(): CopyPair {
    postconditions<CopyPair> { result -> result.first == 10 && result.second == 30 }
    return CopyPair(10, 20).copy(second = 30)
}

// Known gap: same invalid-arity `copy` call; verifies although `copy` has no postcondition linking the result to `pair`.
@AlwaysVerify
fun <!VIPER_TEXT!>copyDefaultsBoundary<!>(pair: CopyPair): CopyPair {
    postconditions<CopyPair> { result -> result.first == pair.first && result.second == pair.second }
    return pair.copy()
}
