// FULL_JDK
import org.jetbrains.kotlin.formver.plugin.NeverVerify

fun <!VIPER_TEXT!>addition<!>(x: Int) {
    val y = x + x
}
fun <!VIPER_TEXT!>subtraction<!>(x: Int) {
    val y = x - x
}
fun <!VIPER_TEXT!>multiplication<!>(x: Int) {
    val y = x * x
}

@NeverVerify // will not verify because `x` is not guaranteed to be non-zero
fun <!VIPER_TEXT!>division<!>(x: Int) {
    val y = x / x
}

@NeverVerify // will not verify because `x` is not guaranteed to be non-zero
fun <!VIPER_TEXT!>remainder<!>(x: Int) {
    val y = x % x
}

fun <!VIPER_TEXT!>unary_minus<!>(x: Int) {
    val y = -x
}
