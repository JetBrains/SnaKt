// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.ADT
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions
import org.jetbrains.kotlin.formver.plugin.verify

@ADT
data object Red

@ADT
data object Blue

@AlwaysVerify
fun <!VIPER_TEXT!>adtIsAny<!>(r: Red): Red {
    postconditions<Red> { res -> <!USELESS_IS_CHECK!>res is Red<!> }
    return r
}

@AlwaysVerify
fun <!VIPER_TEXT!>redIsRed<!>() {
    val r1 = Red
    val r2 = Red
    verify(r1 == r2)
}

fun <!VIPER_TEXT!>redIsNotBlue<!>(r: Red) {
    verify(r != Blue)
}
