// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@Pure
fun <!VIPER_TEXT!>increment<!>(x: Int): Int = x + 1

@Pure
fun <!VIPER_TEXT!>choose<!>(condition: Boolean, whenTrue: Int, whenFalse: Int): Int =
    if (condition) whenTrue else whenFalse

@Pure
fun <!VIPER_TEXT!>negate<!>(x: Int): Int = -x

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>composePureExpressions<!>(condition: Boolean, x: Int): Int {
    postconditions<Int> { result ->
        result == negate(choose(condition, increment(x), -increment(x)))
    }

    val incremented = increment(x)
    val selected = choose(condition, incremented, -incremented)
    return negate(selected)
}
