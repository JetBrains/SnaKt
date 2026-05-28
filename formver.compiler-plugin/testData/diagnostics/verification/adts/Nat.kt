// FULL_JDK

package diagnostics.verification.adts

import org.jetbrains.kotlin.formver.plugin.*

<!ADT_VIOLATION, ADT_VIOLATION, ADT_VIOLATION!>@ADT
sealed interface Nat<!>

@ADT
data <!ADT_VIOLATION, ADT_VIOLATION, ADT_VIOLATION!>object Zero<!> : Nat

<!ADT_VIOLATION, ADT_VIOLATION, ADT_VIOLATION!>@ADT
data class Succ(val pred: Nat) : Nat<!>

<!INTERNAL_ERROR!>@Pure
fun add(m: Nat, n: Nat): Nat = when (m) {
        is Zero -> n
        is Succ -> Succ(add(m.pred, n))
    }<!>

<!ADT_VIOLATION!>@AlwaysVerify
fun natRefl(a: Nat): Unit {
    postconditions<Unit> { a == a }
    when (a) {
        is Zero -> {}
        is Succ -> natRefl(a.pred)
    }
}<!>

<!ADT_VIOLATION!>@AlwaysVerify
fun natTrans(a: Nat, b: Nat, c: Nat): Unit {
    preconditions {
        a == b
        b == c
    }
    postconditions<Unit> { a == c }
    when (a) {
        is Zero -> {}
        is Succ -> when (b) {
            is Zero -> {}
            is Succ -> when (c) {
                is Zero -> {}
                is Succ -> natTrans(a.pred, b.pred, c.pred)
            }
        }
    }
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun conCong(a: Nat, b: Nat): Unit {
    preconditions { a == b }
    postconditions<Unit> { Succ(a) == Succ(b) }
    verify(a == b)
}<!>

<!ADT_VIOLATION!>@AlwaysVerify
fun natSym(a: Nat, b: Nat): Unit {
    preconditions { a == b }
    postconditions<Unit> { b == a }
    when (a) {
        is Zero -> {}
        is Succ -> when (b) {
            is Zero -> {}
            is Succ -> {
                natSym(a.pred, b.pred)
                verify(b.pred == a.pred)
            }
        }
    }
}<!>

<!INTERNAL_ERROR!>fun addCong(a: Nat, b: Nat, c: Nat): Unit {
    preconditions { a == b }
    postconditions<Unit> { add(a, c) == add(b, c) }
    when (a) {
        is Zero -> {
            natRefl(c)
        }
        is Succ -> when (b) {
            is Zero -> {}
            is Succ -> {
                addCong(a.pred, b.pred, c)
                verify(add(a.pred, c) == add(b.pred, c))
            }
        }
    }
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun addCongR(a: Nat, b: Nat, c: Nat): Unit {
    preconditions { b == c }
    postconditions<Unit> { add(a, b) == add(a, c) }
    when (a) {
        is Zero -> {}
        is Succ -> addCongR(a.pred, b, c)
    }
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun addRightZero(m: Nat): Unit {
    postconditions<Unit> { add(m, Zero) == m }
    when (m) {
        is Zero -> {}
        is Succ -> addRightZero(m.pred)
    }
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun addRightSucc(m: Nat, n: Nat): Unit {
    postconditions<Unit> { add(m, Succ(n)) == Succ(add(m, n)) }
    when (m) {
        is Zero -> {
            natRefl(n)
        }
        is Succ -> {
            addRightSucc(m.pred, n)
            verify(add(m.pred, Succ(n)) == Succ(add(m.pred, n)))
        }
    }
}<!>

<!INTERNAL_ERROR!>fun addComm(m: Nat, n: Nat): Unit {
    postconditions<Unit> { add(m, n) == add(n, m) }
    when (m) {
        is Zero -> {
            addRightZero(n)
            natSym(add(n, m), n)
        }
        is Succ -> {
            addComm(m.pred, n)
            verify(add(m.pred, n) == add(n, m.pred))
            conCong(add(m.pred, n), add(n, m.pred))
            verify(Succ(add(m.pred, n)) == Succ(add(n, m.pred)))
            addRightSucc(n, m.pred)
            verify(add(n, Succ(m.pred)) == Succ(add(n, m.pred)))
            natSym(add(n, Succ(m.pred)), Succ(add(n, m.pred)))
            verify(Succ(add(n, m.pred)) == add(n, Succ(m.pred)))
            natTrans(
                add(m, n),
                Succ(add(n, m.pred)),
                add(n, m)
            )
        }
    }
}<!>
