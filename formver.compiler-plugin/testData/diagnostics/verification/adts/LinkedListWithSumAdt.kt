// FULL_JDK

package diagnostics.verification.adts

import org.jetbrains.kotlin.formver.plugin.*

@ADT
sealed interface LinkedList

@ADT
data object Nil : LinkedList

@ADT
data class Cons(val head: Int, val tail: LinkedList) : LinkedList

@Pure
fun <!VIPER_TEXT!>length<!>(xs: LinkedList): Int = when (xs) {
    is Nil -> 0
    is Cons -> 1 + length(xs.tail)
}

@Pure
fun <!VIPER_TEXT!>append<!>(xs: LinkedList, ys: LinkedList): LinkedList = when (xs) {
    is Nil -> ys
    is Cons -> Cons(xs.head, append(xs.tail, ys))
}

@Pure
fun <!VIPER_TEXT!>reverse<!>(xs: LinkedList): LinkedList = when (xs) {
    is Nil -> Nil
    is Cons -> append(reverse(xs.tail), Cons(xs.head, Nil))
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>listRefl<!>(xs: LinkedList): Unit {
    postconditions<Unit> {
        xs == xs
    }
    when (xs) {
        is Nil -> {}
        is Cons -> listRefl(xs.tail)
    }
}<!>

@AlwaysVerify
fun <!VIPER_TEXT!>listSym<!>(a: LinkedList, b: LinkedList): Unit {
    preconditions { a == b }
    postconditions<Unit> {
        b == a
    }
    when (a) {
        is Nil -> {}
        is Cons -> when (b) {
            is Nil -> {}
            is Cons -> <!VIPER_VERIFICATION_ERROR!>listSym(a.tail, b.tail)<!>
        }
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>listTrans<!>(a: LinkedList, b: LinkedList, c: LinkedList): Unit {
    preconditions {
        a == b
        b == c
    }
    postconditions<Unit> {
        a == c
    }
    when (a) {
        is Nil -> {}
        is Cons -> when (b) {
            is Nil -> {}
            is Cons -> when (c) {
                is Nil -> {}
                is Cons -> <!VIPER_VERIFICATION_ERROR!>listTrans(a.tail, b.tail, c.tail)<!>
            }
        }
    }
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>appendNull<!>(xs: LinkedList): Unit {
    postconditions<Unit> {
        append(xs, Nil) == xs
    }
    when (xs) {
        is Nil -> {}
        is Cons -> appendNull(xs.tail)
    }
}<!>

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>appendAssoc<!>(xs: LinkedList, ys: LinkedList, zs: LinkedList): Unit {
    postconditions<Unit> {
        append(append(xs, ys), zs) == append(xs, append(ys, zs))
    }
    when (xs) {
        is Nil -> listRefl(append(ys, zs))
        is Cons -> appendAssoc(xs.tail, ys, zs)
    }
}<!>

@AlwaysVerify
fun <!VIPER_TEXT!>appendCongL<!>(a: LinkedList, b: LinkedList, c: LinkedList): Unit {
    preconditions { a == b }
    postconditions<Unit> {
        append(a, c) == append(b, c)
    }
    when (a) {
        is Nil -> listRefl(c)
        is Cons -> when (b) {
            is Nil -> {}
            is Cons -> <!VIPER_VERIFICATION_ERROR!>appendCongL(a.tail, b.tail, c)<!>
        }
    }
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>appendCongR<!>(a: LinkedList, b: LinkedList, c: LinkedList): Unit {
    preconditions { b == c }
    postconditions<Unit> {
        append(a, b) == append(a, c)
    }
    when (a) {
        is Nil -> listRefl(b)
        is Cons -> appendCongR(a.tail, b, c)
    }
}<!>

@AlwaysVerify
fun <!VIPER_TEXT!>reverseAppend<!>(xs: LinkedList, ys: LinkedList): Unit {
    postconditions<Unit> {
        reverse(append(xs, ys)) == append(reverse(ys), reverse(xs))
    }
    when (xs) {
        is Nil -> {
            appendNull(reverse(ys))
            listSym(append(reverse(ys), Nil), reverse(ys))
        }
        is Cons -> {
            reverseAppend(xs.tail, ys)
            appendCongL(
                reverse(append(xs.tail, ys)),
                append(reverse(ys), reverse(xs.tail)),
                Cons(xs.head, Nil)
            )
            appendAssoc(reverse(ys), reverse(xs.tail), Cons(xs.head, Nil))
            listRefl(reverse(append(xs, ys)))
            listTrans(
                reverse(append(xs, ys)),
                append(reverse(append(xs.tail, ys)), Cons(xs.head, Nil)),
                append(append(reverse(ys), reverse(xs.tail)), Cons(xs.head, Nil))
            )
            listTrans(
                reverse(append(xs, ys)),
                append(append(reverse(ys), reverse(xs.tail)), Cons(xs.head, Nil)),
                append(reverse(ys), append(reverse(xs.tail), Cons(xs.head, Nil)))
            )
        }
    }
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>appendSingletonCons<!>(x: Int, ys: LinkedList): Unit {
    postconditions<Unit> {
        append(Cons(x, Nil), ys) == Cons(x, ys)
    }
    appendNilL(ys)
}<!>


@AlwaysVerify
fun <!VIPER_TEXT!>reverseReverseIsId<!>(xs: LinkedList): Unit {
    postconditions<Unit> {
        reverse(reverse(xs)) == xs
    }
    when (xs) {
        is Nil -> {}
        is Cons -> {
            val h = Cons(xs.head, Nil)
            reverseReverseIsId(xs.tail) // IH: reverse(reverse(xs.tail)) == xs.tail
            reverseAppend(reverse(xs.tail), h)

            reverseSingleton(xs.head) // proves reverse(h) == h

            listRefl(reverse(xs))
            verify(reverse(xs) == append(reverse(xs.tail), h))

            listRefl(reverse(reverse(xs)))
            verify(reverse(reverse(xs)) == reverse(append(reverse(xs.tail), h)))

            appendCongL(reverse(h), h, reverse(reverse(xs.tail)))
            appendCongR(h, reverse(reverse(xs.tail)), xs.tail)

            // reverse(reverse(xs))
            //   == reverse(append(reverse(xs.tail), h))          (def of reverse)
            //   == append(reverse(h), reverse(reverse(xs.tail))) (reverseAppend)
            //   == append(h, reverse(reverse(xs.tail)))          (reverse(h) == h)
            //   == append(h, xs.tail)                            (IH)
            //   == Cons(xs.head, xs.tail) == xs                  (appendSingletonCons + reconstruct)
            listTrans(
                reverse(reverse(xs)),
                reverse(append(reverse(xs.tail), h)),
                append(reverse(h), reverse(reverse(xs.tail)))
            )
            listTrans(
                reverse(reverse(xs)),
                append(reverse(h), reverse(reverse(xs.tail))),
                append(h, reverse(reverse(xs.tail)))
            )
            listTrans(
                reverse(reverse(xs)),
                append(h, reverse(reverse(xs.tail))),
                append(h, xs.tail)
            )

            appendSingletonCons(xs.head, xs.tail)
            listRefl(xs)
            listTrans(append(h, xs.tail), Cons(xs.head, xs.tail), xs)
            listTrans(
                reverse(reverse(xs)),
                append(h, xs.tail),
                xs
            )
        }
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>appendNilL<!>(xs: LinkedList): Unit {
    postconditions<Unit> {
        append(Nil, xs) == xs
    }
    listRefl(xs)
}

@AlwaysVerify
fun <!VIPER_TEXT!>reverseNil<!>(): Unit {
    postconditions<Unit> {
        reverse(Nil) == Nil
    }
    listRefl(Nil)
}

@AlwaysVerify
fun <!VIPER_TEXT!>reverseSingleton<!>(x: Int): Unit {
    postconditions<Unit> {
        reverse(Cons(x, Nil)) == Cons(x, Nil)
    }
    // Chain single-step lemmas: reverse(Cons(x,Nil)) -> append(reverse(Nil), [x]) -> append(Nil, [x]) -> [x]
    // Chain single-step lemmas: reverse(Cons(x, Nil)) -> append(reverse(Nil), [x]) -> append(Nil, [x]) -> [x]
    reverseNil()
    appendCongL(reverse(Nil), Nil, Cons(x, Nil))
    appendNilL(Cons(x, Nil))
    listTrans(
        append(reverse(Nil), Cons(x, Nil)),
        append(Nil, Cons(x, Nil)),
        Cons(x, Nil)
    )
    listRefl(append(reverse(Nil), Cons(x, Nil)))
    listTrans(
        reverse(Cons(x, Nil)),
        append(reverse(Nil), Cons(x, Nil)),
        Cons(x, Nil)
    )
}

