import org.jetbrains.kotlin.formver.plugin.ADT
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions
import org.jetbrains.kotlin.formver.plugin.verify

@ADT
data object Red

@AlwaysVerify
fun <!VIPER_TEXT!>adtIsAny<!>(r: Red): Red {
    postconditions<Red> { res -> <!USELESS_IS_CHECK!>res is Red<!> }
    return r
}
