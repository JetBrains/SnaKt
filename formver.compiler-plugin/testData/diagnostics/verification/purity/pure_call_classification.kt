// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>classifiedPureIncrement<!>(x: Int): Int = x + 1

fun <!VIPER_TEXT!>unclassifiedIncrement<!>(x: Int): Int = x + 1

@Pure
fun <!VIPER_TEXT!>callClassifiedPureExpression<!>(condition: Boolean, x: Int): Int {
    val adjusted = classifiedPureIncrement(x)
    return if (condition) -adjusted else adjusted
}

<!INTERNAL_ERROR!>@Pure
fun callUnclassifiedExpression(x: Int): Int {
    val adjusted = unclassifiedIncrement(x)
    return adjusted
}<!>
