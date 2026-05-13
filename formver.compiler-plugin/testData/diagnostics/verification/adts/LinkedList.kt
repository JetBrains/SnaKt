// FULL_JDK

package diagnostics.verification.adts

import org.jetbrains.kotlin.formver.plugin.*

@ADT
data class Node(val head: Int, val tail: Node?)

@Pure
fun <!VIPER_TEXT!>length<!>(xs: Node?): Int =
if (xs == null) 0 else 1 + length(xs.tail)

@Pure
fun <!VIPER_TEXT!>append<!>(xs: Node?, ys: Node?): Node? =
if (xs == null) ys else Node(xs.head, append(xs.tail, ys))

@Pure
fun <!VIPER_TEXT!>reverse<!>(xs: Node?): Node? =
if (xs == null) null else append(reverse(xs.tail), Node(xs.head, null))

@Pure
fun <!VIPER_TEXT!>allNonNeg<!>(xs: Node?): Boolean =
if (xs == null) true else xs.head >= 0 && allNonNeg(xs.tail)

@Pure
fun <!VIPER_TEXT!>sumOf<!>(xs: Node?): Int =
if (xs == null) 0 else xs.head + sumOf(xs.tail)

@Pure
fun <!VIPER_TEXT!>prepend<!>(xs: Node?, x: Int): Node = Node(x, xs)

@AlwaysVerify
fun <!VIPER_TEXT!>lengthNonNeg<!>(xs: Node?): Unit {
    postconditions<Unit> { _ ->
        length(xs) >= 0
    }
    if (xs != null) lengthNonNeg(xs.tail)
}

@AlwaysVerify
fun <!VIPER_TEXT!>appendLength<!>(xs: Node?, ys: Node?): Unit {
    postconditions<Unit> { _ ->
        length(append(xs, ys)) == length(xs) + length(ys)
    }
    if (xs != null) appendLength(xs.tail, ys)
}

@AlwaysVerify
fun <!VIPER_TEXT!>appendNull<!>(xs: Node?): Unit {
    postconditions<Unit> { _ ->
        append(xs, null) == xs
    }
    if (xs != null) appendNull(xs.tail)
}

@AlwaysVerify
fun <!VIPER_TEXT!>nodeRefl<!>(xs: Node?): Unit {
    postconditions<Unit> { _ ->
        xs == xs
    }
    if (xs != null) nodeRefl(xs.tail)
}

@AlwaysVerify
fun <!VIPER_TEXT!>appendAssoc<!>(xs: Node?, ys: Node?, zs: Node?): Unit {
    postconditions<Unit> { _ ->
        append(append(xs, ys), zs) == append(xs, append(ys, zs))
    }
    if (xs != null) appendAssoc(xs.tail, ys, zs)
    else nodeRefl(append(ys, zs))
}

@AlwaysVerify
fun <!VIPER_TEXT!>reverseLength<!>(xs: Node?): Unit {
    postconditions<Unit> { _ ->
        length(reverse(xs)) == length(xs)
    }
    if (xs != null) {
        reverseLength(xs.tail)
        appendLength(reverse(xs.tail), Node(xs.head, null))
        verify(length(reverse(xs)) ==
                length(append(reverse(xs.tail), Node(xs.head, null))))
        verify(length(null) == 0)
        verify(length(Node(xs.head, null)) == 1)
        verify(length(xs) == 1 + length(xs.tail))
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>node_refl<!>(r: Node): Unit {
    postconditions<Unit> { _ ->
        r == r
    }
    val t = r.tail
    if (t != null) {
        node_refl(t)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>node_sym<!>(a: Node?, b: Node?): Unit {
    preconditions {
        a == b
    }
    postconditions<Unit> { _ ->
        b == a
    }
    if (a != null) {
        if (b != null) {
            val ta = a.tail
            val tb = b.tail
            if (ta != null) {
                if (tb != null) {
                    verify(ta == tb)
                    node_sym(ta, tb)
                }
            }
        }
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>node_trans<!>(a: Node?, b: Node?, c: Node?): Unit {
    preconditions {
        a == b
        b == c
    }
    postconditions<Unit> { _ ->
        a == c
    }
    if (a != null) {
        if (b != null) {
            if (c != null) {
                val ta = a.tail
                val tb = b.tail
                val tc = c.tail
                if (ta != null) {
                    if (tb != null) {
                        if (tc != null) {
                            verify(ta == tb)
                            verify(tb == tc)
                            node_trans(ta, tb, tc)
                        }
                    }
                }
            }
        }
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>appendCongL<!>(a: Node?, b: Node?, c: Node?): Unit {
    preconditions {
        a == b
    }
    postconditions<Unit> { _ ->
        append(a, c) == append(b, c)
    }
    if (a != null) {
        if (b != null) {
            val ta = a.tail
            val tb = b.tail
            if (ta != null) {
                if (tb != null) {
                    verify(ta == tb)
                    appendCongL(ta, tb, c)
                }
            } else {
                if (c != null) {
                    node_refl(c)
                }
                verify(append(ta, c) == c)
                verify(append(tb, c) == c)
            }
            val tailA = append(ta, c)
            if (tailA != null) {
                node_refl(tailA)
            }
            val tailB = append(tb, c)
            if (tailB != null) {
                node_refl(tailB)
            }
            val ac = append(a, c)
            val bc = append(b, c)
            if (ac != null) {
                if (bc != null) {
                    verify(ac.head == a.head)
                    verify(bc.head == b.head)
                    verify(ac.tail == append(ta, c))
                    verify(bc.tail == append(tb, c))
                }
            }
        }
    } else {
        if (c != null) {
            node_refl(c)
        }
        verify(append(a, c) == c)
        verify(append(b, c) == c)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>reverseAppend<!>(xs: Node?, ys: Node?): Unit {
    postconditions<Unit> { _ ->
        reverse(append(xs, ys)) == append(reverse(ys), reverse(xs))
    }
    if (xs == null) {
        appendNull(reverse(ys))
        node_sym(append(reverse(ys), null), reverse(ys))
    } else {
        reverseAppend(xs.tail, ys)
        appendCongL(
            reverse(append(xs.tail, ys)),
            append(reverse(ys), reverse(xs.tail)),
            Node(xs.head, null)
        )
        appendAssoc(reverse(ys), reverse(xs.tail), Node(xs.head, null))
        node_trans(
            append(reverse(append(xs.tail, ys)), Node(xs.head, null)),
            append(append(reverse(ys), reverse(xs.tail)), Node(xs.head, null)),
            append(reverse(ys), append(reverse(xs.tail), Node(xs.head, null)))
        )
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>appendCongR<!>(a: Node?, b: Node?, c: Node?): Unit {
    preconditions {
        b == c
    }
    postconditions<Unit> { _ ->
        append(a, b) == append(a, c)
    }
    if (a != null) {
        appendCongR(a.tail, b, c)
        val tailB = append(a.tail, b)
        val tailC = append(a.tail, c)
        if (tailB != null) {
            node_refl(tailB)
        }
        if (tailC != null) {
            node_refl(tailC)
        }
        val ab = append(a, b)
        val ac = append(a, c)
        if (ab != null) {
            if (ac != null) {
                verify(ab.head == a.head)
                verify(ac.head == a.head)
                verify(ab.tail == tailB)
                verify(ac.tail == tailC)
            }
        }
    } else {
        if (b != null) {
            node_refl(b)
        }
        if (c != null) {
            node_refl(c)
        }
        verify(append(a, b) == b)
        verify(append(a, c) == c)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>reverseReverseIsId<!>(xs: Node?): Unit {
    postconditions<Unit> { _ ->
        reverse(reverse(xs)) == xs
    }
    if (xs != null) {
        val h = Node(xs.head, null)
        reverseReverseIsId(xs.tail)
        reverseAppend(reverse(xs.tail), h)

        node_refl(h)
        node_refl(xs)
        val xst = xs.tail
        if (xst != null) {
            node_refl(xst)
        }
        val rxs = reverse(xs)
        if (rxs != null) {
            node_refl(rxs)
        }
        val rrxs = reverse(reverse(xs))
        if (rrxs != null) {
            node_refl(rrxs)
        }
        val rar = reverse(append(reverse(xs.tail), h))
        if (rar != null) {
            node_refl(rar)
        }

        val singleton = Node(h.head, null)
        if (<!SENSELESS_COMPARISON!>singleton != null<!>) {
            node_refl(singleton)
        }
        verify(reverse(h.tail) == null)
        verify(append(reverse(h.tail), singleton) == singleton)
        verify(reverse(h) == append(reverse(h.tail), singleton))
        verify(singleton == h)
        node_trans(
            reverse(h),
            append(reverse(h.tail), singleton),
            singleton
        )
        node_trans(reverse(h), singleton, h)
        verify(reverse(h) == h)

        verify(reverse(xs) == append(reverse(xs.tail), h))
        verify(reverse(reverse(xs)) == reverse(append(reverse(xs.tail), h)))

        verify(append(h.tail, xs.tail) == xs.tail)
        val hxst = append(h, xs.tail)
        if (hxst != null) {
            verify(hxst.head == xs.head)
            verify(hxst.tail == append(h.tail, xs.tail))
        }
        verify(append(h, xs.tail) == xs)

        appendCongL(reverse(h), h, reverse(reverse(xs.tail)))
        appendCongR(h, reverse(reverse(xs.tail)), xs.tail)

        node_trans(
            reverse(reverse(xs)),
            reverse(append(reverse(xs.tail), h)),
            append(reverse(h), reverse(reverse(xs.tail)))
        )
        node_trans(
            reverse(reverse(xs)),
            append(reverse(h), reverse(reverse(xs.tail))),
            append(h, reverse(reverse(xs.tail)))
        )
        node_trans(
            reverse(reverse(xs)),
            append(h, reverse(reverse(xs.tail))),
            append(h, xs.tail)
        )
        node_trans(
            reverse(reverse(xs)),
            append(h, xs.tail),
            xs
        )
    }
}
