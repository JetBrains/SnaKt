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

// Branch bodies of a total `when` are still checked: `r` can be negative.
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

// Known gap: a pure body keeps a `Unit` fallthrough, and Viper cannot rule it out for a sealed subject,
// so the result-type postcondition fails.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>pureSealedWhen<!>(e: Expr): Boolean = when (e) {
    is Const -> true
    is Neg -> false
}<!>
