// FULL_JDK

package diagnostics.verification.adts

import org.jetbrains.kotlin.formver.plugin.*

@ADT
sealed interface Expr

@ADT
data class Lit(val n: Int) : Expr

@ADT
data class Add(val left: Expr, val right: Expr) : Expr

@ADT
data class Neg(val expr: Expr) : Expr

@Pure
fun <!VIPER_TEXT!>eval<!>(e: Expr): Int = when (e) {
        is Lit -> e.n
        is Add -> eval(e.left) + eval(e.right)
        is Neg -> -eval(e.expr)
    }

@AlwaysVerify
fun <!VIPER_TEXT!>evalNeg<!>(e: Expr): Unit {
    postconditions<Unit> {
        eval(Neg(e)) == -eval(e)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>evalAdd<!>(a: Expr, b: Expr): Unit {
    postconditions<Unit> {
        eval(Add(a, b)) == eval(a) + eval(b)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>doubleNegIsId<!>(e: Expr): Unit {
    postconditions<Unit> {
        eval(Neg(Neg(e))) == eval(e)
    }
    // Each evalNeg unfolds one level
    evalNeg(Neg(e))
    evalNeg(e)
}

@AlwaysVerify
fun <!VIPER_TEXT!>addEvalComm<!>(a: Expr, b: Expr): Unit {
    postconditions<Unit> {
        eval(Add(a, b)) == eval(Add(b, a))
    }
    evalAdd(a, b)
    evalAdd(b, a)
}

@AlwaysVerify
fun <!VIPER_TEXT!>negDistributesAdd<!>(a: Expr, b: Expr): Unit {
    postconditions<Unit> {
        eval(Neg(Add(a, b))) == eval(Add(Neg(a), Neg(b)))
    }
    // Unfold eval for each subexpression
    evalNeg(Add(a, b))
    evalAdd(a, b)
    evalNeg(a)
    evalNeg(b)
    evalAdd(Neg(a), Neg(b))
}
