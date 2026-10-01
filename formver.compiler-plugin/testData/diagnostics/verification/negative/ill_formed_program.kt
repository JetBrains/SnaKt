// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

data class Pair(val first: Int, val second: Int)

// The conversion drops the default arguments of `copy`, so the Viper method call has too few arguments.
// Silver's consistency check rejects the program, and it is not verified.
@AlwaysVerify
fun <!VIPER_TEXT!>copyWithDefaults<!>(pair: Pair): Pair {
    return <!VIPER_VERIFICATION_ABORTED!>pair.copy(second = 30)<!>
}
