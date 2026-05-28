// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

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
