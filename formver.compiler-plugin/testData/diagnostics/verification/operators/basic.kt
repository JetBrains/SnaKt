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

fun <!VIPER_TEXT!>less<!>(x: Int, y: Int): Boolean {
    return x < y
}

fun <!VIPER_TEXT!>lessEqual<!>(x: Int, y: Int): Boolean {
    return x <= y
}

fun <!VIPER_TEXT!>greater<!>(x: Int, y: Int): Boolean {
    return x > y
}

fun <!VIPER_TEXT!>greaterEqual<!>(x: Int, y: Int): Boolean {
    return x >= y
}

fun <!VIPER_TEXT!>negation<!>(x: Boolean): Boolean {
    return !x
}

fun <!VIPER_TEXT!>conjunction<!>(x: Boolean, y: Boolean): Boolean {
    return x && y
}

fun <!VIPER_TEXT!>conjunctionSideEffects<!>(x: Boolean, y: Boolean): Boolean {
    // This does not actually have side effects, but the code should compile as if it might.
    return negation(x) && negation(y)
}

fun <!VIPER_TEXT!>disjunction<!>(x: Boolean, y: Boolean): Boolean {
    return x || y
}

fun <!VIPER_TEXT!>test_simple<!>() {
    var x = 10
    x++
    x--
    ++x
    --x
}

fun <!VIPER_TEXT!>test_postincvrement<!>() {
    var x = 10
    val first = x++
    val second = x--
    val unary = x
}
