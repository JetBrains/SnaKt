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

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>pureSealedWhen<!>(e: Expr): Boolean = when (e) {
    is Const -> true
    is Neg -> false
}

@AlwaysVerify
fun <!VIPER_TEXT!>booleanWhen<!>(b: Boolean): Int = when (b) {
    true -> 1
    false -> 0
}

@AlwaysVerify
fun <!VIPER_TEXT!>nullableSealedWhen<!>(e: Expr?): Int = when (e) {
    null -> 0
    is Const -> 1
    is Neg -> 2
}

sealed class Shape
object Dot : Shape()
sealed class Polygon : Shape()
class Triangle : Polygon()
class Square : Polygon()

// The fallthrough is proved unreachable through the nested sealed `Polygon`.
@AlwaysVerify
fun <!VIPER_TEXT!>nestedSealedWhen<!>(s: Shape): Int = when (s) {
    is Dot -> 0
    is Triangle -> 3
    is Square -> 4
}

@AlwaysVerify
fun <!VIPER_TEXT!>coarseSealedWhen<!>(s: Shape): Int = when (s) {
    is Dot -> 0
    is Polygon -> 1
}

// The sealed hierarchy axioms do not make a value of a sealed type impossible.
@AlwaysVerify
fun <!VIPER_TEXT!>axiomsConsistent<!>(s: Shape) {
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

// Without a branch for `Square`, the `else` branch is reachable.
@AlwaysVerify
fun <!VIPER_TEXT!>nonExhaustiveElse<!>(s: Shape): Int {
    val r = when (s) {
        is Dot -> 0
        is Triangle -> 3
        else -> 4
    }
    verify(<!VIPER_VERIFICATION_ERROR!>r != 4<!>)
    return r
}

// A subject typed by a type parameter embeds as `Any?`, so its fallthrough is trusted rather than checked.
@AlwaysVerify
fun <T : Expr> <!VIPER_TEXT!>genericSealedWhen<!>(e: T): Int = when (e) {
    is Const -> 1
    is Neg -> 2
}
