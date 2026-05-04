// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.ADT
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions
import org.jetbrains.kotlin.formver.plugin.verify

@ADT
data object Red

@Suppress("SENSELESS_COMPARISON")
@AlwaysVerify
fun <!VIPER_TEXT!>nullableRed<!>(): Red? {
    var r: Red? = null
    r = Red
    verify(r != null)
    return r
}

@AlwaysVerify
fun <!VIPER_TEXT!>returnsRed<!>(): Red {
    val x = Red
    verify(x == Red)
    return x
}
