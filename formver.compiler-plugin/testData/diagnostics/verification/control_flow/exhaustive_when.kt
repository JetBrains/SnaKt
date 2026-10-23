// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Pure
import org.jetbrains.kotlin.formver.plugin.verify

sealed interface Expr
class Const(val value: Int) : Expr
class Neg(val operand: Const) : Expr

// An exhaustive `when` over a sealed interface with no `else` verifies as total.
@AlwaysVerify
fun <!VIPER_TEXT!>eval<!>(e: Expr): Int = when (e) {
    is Const -> e.value
    is Neg -> -e.operand.value
}

// Exhaustiveness does not imply a non-negative result: either variant can produce a negative value.
@AlwaysVerify
fun <!VIPER_TEXT!>evalNonNeg<!>(e: Expr): Int {
    val r = when (e) {
        is Const -> e.value
        is Neg -> -e.operand.value
    }
    verify(<!VIPER_VERIFICATION_ERROR!>r >= 0<!>)
    return r
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>pureBooleanWhen<!>(b: Boolean): Int = when (b) {
    true -> 1
    false -> 0
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>pureBooleanWhenStatement<!>(b: Boolean): Int {
    var r = 0
    when (b) {
        true -> r = 1
        false -> r = 2
    }
    return r
}
