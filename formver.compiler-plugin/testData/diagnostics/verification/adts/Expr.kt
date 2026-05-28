// FULL_JDK

package diagnostics.verification.adts

import org.jetbrains.kotlin.formver.plugin.*

<!ADT_VIOLATION!>@ADT
sealed interface Expr<!>

<!ADT_VIOLATION!>@ADT
data class Lit(val n: Int) : Expr<!>

<!ADT_VIOLATION!>@ADT
data class Add(val left: Expr, val right: Expr) : Expr<!>

<!ADT_VIOLATION!>@ADT
data class Neg(val expr: Expr) : Expr<!>

<!ADT_VIOLATION!>@Pure
fun eval(e: Expr): Int = when (e) {
        is Lit -> e.n
        is Add -> eval(e.left) + eval(e.right)
        is Neg -> -eval(e.expr)
    }<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun evalNeg(e: Expr): Unit {
    postconditions<Unit> {
        eval(Neg(e)) == -eval(e)
    }
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun evalAdd(a: Expr, b: Expr): Unit {
    postconditions<Unit> {
        eval(Add(a, b)) == eval(a) + eval(b)
    }
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun doubleNegIsId(e: Expr): Unit {
    postconditions<Unit> {
        eval(Neg(Neg(e))) == eval(e)
    }
    // Each evalNeg unfolds one level
    evalNeg(Neg(e))
    evalNeg(e)
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun addEvalComm(a: Expr, b: Expr): Unit {
    postconditions<Unit> {
        eval(Add(a, b)) == eval(Add(b, a))
    }
    evalAdd(a, b)
    evalAdd(b, a)
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun negDistributesAdd(a: Expr, b: Expr): Unit {
    postconditions<Unit> {
        eval(Neg(Add(a, b))) == eval(Add(Neg(a), Neg(b)))
    }
    // Unfold eval for each subexpression
    evalNeg(Add(a, b))
    evalAdd(a, b)
    evalNeg(a)
    evalNeg(b)
    evalAdd(Neg(a), Neg(b))
}<!>
