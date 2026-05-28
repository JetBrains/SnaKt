// FULL_JDK

package diagnostics.verification.adts

import org.jetbrains.kotlin.formver.plugin.*

@ADT
sealed interface Option

@ADT
data object None : Option

@ADT
data class Some(val value: Int) : Option

@Pure
fun <!VIPER_TEXT!>isSome<!>(x: Option): Boolean = when (x) {
    is None -> false
    is Some -> true
}

@Pure
fun <!VIPER_TEXT!>getOrElse<!>(x: Option, default: Int): Int = when (x) {
    is None -> default
    is Some -> x.value
}

@Pure
fun <!VIPER_TEXT!>orElse<!>(a: Option, b: Option): Option = when (a) {
    is None -> b
    is Some -> a
}

@AlwaysVerify
fun <!VIPER_TEXT!>optRefl<!>(x: Option): Unit {
    postconditions<Unit> { x == x }
    when (x) {
        is None -> {}
        is Some -> {}
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>orElseIdempotent<!>(opt: Option): Unit {
    postconditions<Unit> {
        orElse(opt, opt) == opt
    }
    when (opt) {
        is None -> {}
        is Some -> {}
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>orElseAssoc<!>(a: Option, b: Option, c: Option): Unit {
    postconditions<Unit> {
        orElse(orElse(a, b), c) == orElse(a, orElse(b, c))
    }
    when (a) {
        is None -> optRefl(orElse(b, c))  // Both sides reduce to orElse(b, c), seed reflexivity.
        is Some -> optRefl(a)  // Both sides reduce to a, seed reflexivity.
    }
}

@Pure
fun <!VIPER_TEXT!>safeHead<!>(xs: LinkedList): Option = when (xs) {
    is Nil -> None
    is Cons -> Some(xs.head)
}

@AlwaysVerify
fun <!VIPER_TEXT!>safeHeadAppend<!>(xs: LinkedList, ys: LinkedList): Unit {
    postconditions<Unit> {
        safeHead(append(xs, ys)) == orElse(safeHead(xs), safeHead(ys))
    }
    when (xs) {
        is Nil -> optRefl(safeHead(ys))  // safeHead(ys) == orElse(None, safeHead(ys)), seed reflexivity.
        is Cons -> optRefl(safeHead(xs))  // safeHead(Cons) == Some(head) == orElse(Some(head), _), seed reflexivity.
    }
}

// LinkedList ADT Section
// We re-introduce definitions, functions and lemmas from LinkedListWithSumAdt.kt to use them in proofs

@ADT
sealed interface LinkedList

@ADT
data object Nil : LinkedList

@ADT
data class Cons(val head: Int, val tail: LinkedList) : LinkedList

@Pure
fun <!VIPER_TEXT!>append<!>(xs: LinkedList, ys: LinkedList): LinkedList = when (xs) {
    is Nil -> ys
    is Cons -> Cons(xs.head, append(xs.tail, ys))
}
