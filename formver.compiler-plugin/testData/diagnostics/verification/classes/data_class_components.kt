// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

data class ComponentPair(val first: Int, val second: Int)

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>componentOne<!>(): Int {
    postconditions<Int> { result -> result == 10 }
    return ComponentPair(10, 20).component1()
}<!>

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>componentTwoBoundary<!>(pair: ComponentPair): Int {
    postconditions<Int> { result -> result == pair.second }
    return pair.component2()
}<!>
