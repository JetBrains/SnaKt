// FULL_JDK

package diagnostics.verification.adts

import org.jetbrains.kotlin.formver.plugin.*

@ADT
data class PBST(val key: Int, val value: Int, val left: PBST?, val right: PBST?)

@ADT
data class KVList(val key: Int, val value: Int, val tail: KVList?)

@Pure
fun <!VIPER_TEXT!>size<!>(t: PBST?): Int =
    if (t == null) 0 else 1 + size(t.left) + size(t.right)

@Pure
fun <!VIPER_TEXT!>lookup<!>(t: PBST?, k: Int): Int =
    if (t == null) 0
    else if (k < t.key) lookup(t.left, k)
    else if (k > t.key) lookup(t.right, k)
    else t.value

@Pure
fun <!VIPER_TEXT!>insert<!>(t: PBST?, k: Int, v: Int): PBST =
    if (t == null) PBST(k, v, null, null)
    else if (k < t.key) PBST(t.key, t.value, insert(t.left, k, v), t.right)
    else if (k > t.key) PBST(t.key, t.value, t.left, insert(t.right, k, v))
    else PBST(k, v, t.left, t.right)

// BST invariant via per-subtree key bounds
@Pure
fun <!VIPER_TEXT!>allLt<!>(t: PBST?, b: Int): Boolean =
    if (t == null) true
    else t.key < b && allLt(t.left, b) && allLt(t.right, b)

@Pure
fun <!VIPER_TEXT!>allGt<!>(t: PBST?, b: Int): Boolean =
    if (t == null) true
    else t.key > b && allGt(t.left, b) && allGt(t.right, b)

@Pure
fun <!VIPER_TEXT!>isBST<!>(t: PBST?): Boolean =
    if (t == null) true
    else allLt(t.left, t.key) && allGt(t.right, t.key)
    && isBST(t.left) && isBST(t.right)

@Pure
fun <!VIPER_TEXT!>appendKV<!>(xs: KVList?, ys: KVList?): KVList? =
    if (xs == null) ys
    else KVList(xs.key, xs.value, appendKV(xs.tail, ys))

@Pure
fun <!VIPER_TEXT!>inOrder<!>(t: PBST?): KVList? =
    if (t == null) null
    else appendKV(inOrder(t.left), KVList(t.key, t.value, inOrder(t.right)))

@Pure
fun <!VIPER_TEXT!>sortedInsert<!>(xs: KVList?, k: Int, v: Int): KVList? =
    if (xs == null) KVList(k, v, null)
    else if (k < xs.key) KVList(k, v, xs)
    else if (k > xs.key) KVList(xs.key, xs.value, sortedInsert(xs.tail, k, v))
    else KVList(k, v, xs.tail)

@Pure
fun <!VIPER_TEXT!>kvAllLt<!>(xs: KVList?, b: Int): Boolean =
    if (xs == null) true
    else xs.key < b && kvAllLt(xs.tail, b)

@Pure
fun <!VIPER_TEXT!>kvAllGt<!>(xs: KVList?, b: Int): Boolean =
    if (xs == null) true
    else xs.key > b && kvAllGt(xs.tail, b)

@AlwaysVerify
fun <!VIPER_TEXT!>tree_refl<!>(t: PBST?): Unit {
    postconditions<Unit> { t == t }
    if (t != null) {
        tree_refl(t.left)
        tree_refl(t.right)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>kvlist_refl<!>(xs: KVList?): Unit {
    postconditions<Unit> { xs == xs }
    if (xs != null) kvlist_refl(xs.tail)
}

@AlwaysVerify
fun <!VIPER_TEXT!>kvlist_sym<!>(a: KVList?, b: KVList?): Unit {
    preconditions { a == b }
    postconditions<Unit> { b == a }
    if (a != null) {
        if (b != null) {
            val ta = a.tail
            val tb = b.tail
            if (ta != null) {
                if (tb != null) {
                    verify(ta == tb)
                    kvlist_sym(ta, tb)
                }
            }
        }
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>kvlist_trans<!>(a: KVList?, b: KVList?, c: KVList?): Unit {
    preconditions {
        a == b
        b == c
    }
    postconditions<Unit> { a == c }
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
                            kvlist_trans(ta, tb, tc)
                        }
                    }
                }
            }
        }
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>appendKV_congL<!>(a: KVList?, b: KVList?, c: KVList?): Unit {
    preconditions { a == b }
    postconditions<Unit> { appendKV(a, c) == appendKV(b, c) }
    if (a != null) {
        if (b != null) {
            val ta = a.tail
            val tb = b.tail
            if (ta != null) {
                if (tb != null) {
                    verify(ta == tb)
                    appendKV_congL(ta, tb, c)
                }
            } else {
                if (c != null) kvlist_refl(c)
                verify(appendKV(ta, c) == c)
                verify(appendKV(tb, c) == c)
            }
            val tailA = appendKV(ta, c)
            if (tailA != null) kvlist_refl(tailA)
            val tailB = appendKV(tb, c)
            if (tailB != null) kvlist_refl(tailB)
            val ac = appendKV(a, c)
            val bc = appendKV(b, c)
            if (ac != null) {
                if (bc != null) {
                    verify(ac.key == a.key)
                    verify(bc.key == b.key)
                    verify(ac.value == a.value)
                    verify(bc.value == b.value)
                    verify(ac.tail == appendKV(ta, c))
                    verify(bc.tail == appendKV(tb, c))
                }
            }
        }
    } else {
        if (c != null) kvlist_refl(c)
        verify(appendKV(a, c) == c)
        verify(appendKV(b, c) == c)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>appendKV_congR<!>(a: KVList?, b: KVList?, c: KVList?): Unit {
    preconditions { b == c }
    postconditions<Unit> { appendKV(a, b) == appendKV(a, c) }
    if (a != null) {
        appendKV_congR(a.tail, b, c)
        val tailB = appendKV(a.tail, b)
        val tailC = appendKV(a.tail, c)
        if (tailB != null) kvlist_refl(tailB)
        if (tailC != null) kvlist_refl(tailC)
        val ab = appendKV(a, b)
        val ac = appendKV(a, c)
        if (ab != null) {
            if (ac != null) {
                verify(ab.key == a.key)
                verify(ac.key == a.key)
                verify(ab.tail == tailB)
                verify(ac.tail == tailC)
            }
        }
    } else {
        if (b != null) kvlist_refl(b)
        if (c != null) kvlist_refl(c)
        verify(appendKV(a, b) == b)
        verify(appendKV(a, c) == c)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>sizeNonNeg<!>(t: PBST?): Unit {
    postconditions<Unit> { size(t) >= 0 }
    if (t != null) {
        sizeNonNeg(t.left)
        sizeNonNeg(t.right)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>lookupAfterInsert<!>(t: PBST?, k: Int, v: Int): Unit {
    postconditions<Unit> { lookup(insert(t, k, v), k) == v }
    if (t != null) {
        if (k < t.key) lookupAfterInsert(t.left, k, v)
        else if (k > t.key) lookupAfterInsert(t.right, k, v)
        // k == t.key: insert replaces value, lookup returns v directly
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>insertOverwrite<!>(t: PBST?, k: Int, v1: Int, v2: Int): Unit {
    postconditions<Unit> {
        insert(insert(t, k, v1), k, v2) == insert(t, k, v2)
    }
    if (t == null) {
        val both = PBST(k, v2, null, null)
        tree_refl(both)
        val firstInsert = PBST(k, v1, null, null)
        tree_refl(firstInsert)
        verify(insert(null as PBST?, k, v1) == firstInsert)
        verify(insert(firstInsert, k, v2) == both)
        verify(insert(null as PBST?, k, v2) == both)
    } else {
        if (k < t.key) {
            insertOverwrite(t.left, k, v1, v2)
            tree_refl(t.right)

            tree_refl(insert(insert(t.left, k, v1), k, v2))
            tree_refl(insert(t.left, k, v2))
            val lhs = insert(insert(t, k, v1), k, v2)
            val rhs = insert(t, k, v2)
            verify(lhs.key == t.key)
            verify(rhs.key == t.key)
            verify(lhs.value == t.value)
            verify(rhs.value == t.value)
            verify(lhs.left == insert(insert(t.left, k, v1), k, v2))
            verify(rhs.left == insert(t.left, k, v2))
            verify(lhs.right == t.right)
            verify(rhs.right == t.right)
        } else if (k > t.key) {
            insertOverwrite(t.right, k, v1, v2)
            tree_refl(t.left)
            tree_refl(insert(insert(t.right, k, v1), k, v2))
            tree_refl(insert(t.right, k, v2))
            val lhs = insert(insert(t, k, v1), k, v2)
            val rhs = insert(t, k, v2)
            verify(lhs.key == t.key)
            verify(rhs.key == t.key)
            verify(lhs.value == t.value)
            verify(rhs.value == t.value)
            verify(lhs.left == t.left)
            verify(rhs.left == t.left)
            verify(lhs.right == insert(insert(t.right, k, v1), k, v2))
            verify(rhs.right == insert(t.right, k, v2))
        } else {
            tree_refl(t.left)
            tree_refl(t.right)
            val both = PBST(k, v2, t.left, t.right)
            tree_refl(both)
            verify(insert(insert(t, k, v1), k, v2) == both)
            verify(insert(t, k, v2) == both)
        }
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>allLt_insert<!>(t: PBST?, k: Int, v: Int, b: Int): Unit {
    preconditions {
        allLt(t, b)
        k < b
    }
    postconditions<Unit> { allLt(insert(t, k, v), b) }
    if (t != null) {
        if (k < t.key) allLt_insert(t.left, k, v, b)
        else if (k > t.key) allLt_insert(t.right, k, v, b)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>allGt_insert<!>(t: PBST?, k: Int, v: Int, b: Int): Unit {
    preconditions {
        allGt(t, b)
        k > b
    }
    postconditions<Unit> { allGt(insert(t, k, v), b) }
    if (t != null) {
        if (k < t.key) allGt_insert(t.left, k, v, b)
        else if (k > t.key) allGt_insert(t.right, k, v, b)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>bstInsert<!>(t: PBST?, k: Int, v: Int): Unit {
    preconditions { isBST(t) }
    postconditions<Unit> { isBST(insert(t, k, v)) }
    if (t != null) {
        if (k < t.key) {
            bstInsert(t.left, k, v)
            allLt_insert(t.left, k, v, t.key)
        } else if (k > t.key) {
            bstInsert(t.right, k, v)
            allGt_insert(t.right, k, v, t.key)
        }
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>kvAllGt_relax<!>(xs: KVList?, b: Int, bLoose: Int): Unit {
    preconditions {
        kvAllGt(xs, b)
        b > bLoose
    }
    postconditions<Unit> { kvAllGt(xs, bLoose) }
    if (xs != null) kvAllGt_relax(xs.tail, b, bLoose)
}

@AlwaysVerify
fun <!VIPER_TEXT!>kvAllLt_relax<!>(xs: KVList?, b: Int, bLoose: Int): Unit {
    preconditions {
        kvAllLt(xs, b)
        b < bLoose
    }
    postconditions<Unit> { kvAllLt(xs, bLoose) }
    if (xs != null) kvAllLt_relax(xs.tail, b, bLoose)
}

@AlwaysVerify
fun <!VIPER_TEXT!>kvAllGt_appendKV<!>(xs: KVList?, ys: KVList?, b: Int): Unit {
    preconditions {
        kvAllGt(xs, b)
        kvAllGt(ys, b)
    }
    postconditions<Unit> { kvAllGt(appendKV(xs, ys), b) }
    if (xs != null) kvAllGt_appendKV(xs.tail, ys, b)
}

@AlwaysVerify
fun <!VIPER_TEXT!>kvAllLt_appendKV<!>(xs: KVList?, ys: KVList?, b: Int): Unit {
    preconditions {
        kvAllLt(xs, b)
        kvAllLt(ys, b)
    }
    postconditions<Unit> { kvAllLt(appendKV(xs, ys), b) }
    if (xs != null) kvAllLt_appendKV(xs.tail, ys, b)
}

@AlwaysVerify
fun <!VIPER_TEXT!>allGt_inOrder<!>(t: PBST?, b: Int): Unit {
    preconditions { allGt(t, b) }
    postconditions<Unit> { kvAllGt(inOrder(t), b) }
    if (t != null) {
        allGt_inOrder(t.left, b)
        allGt_inOrder(t.right, b)
        kvAllGt_appendKV(
            inOrder(t.left),
            KVList(t.key, t.value, inOrder(t.right)),
            b
        )
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>allLt_inOrder<!>(t: PBST?, b: Int): Unit {
    preconditions { allLt(t, b) }
    postconditions<Unit> { kvAllLt(inOrder(t), b) }
    if (t != null) {
        allLt_inOrder(t.left, b)
        allLt_inOrder(t.right, b)
        kvAllLt_appendKV(
            inOrder(t.left),
            KVList(t.key, t.value, inOrder(t.right)),
            b
        )
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>sortedInsert_skipRight<!>(xs: KVList?, ys: KVList?, k: Int, v: Int): Unit {
    preconditions { kvAllGt(ys, k) }
    postconditions<Unit> {
        sortedInsert(appendKV(xs, ys), k, v) == appendKV(sortedInsert(xs, k, v), ys)
    }
    if (xs == null) {
        if (ys == null) {
            kvlist_refl(KVList(k, v, null))
        } else {
            kvlist_refl(KVList(k, v, ys))
        }
    } else {
        if (k < xs.key) {
            kvlist_refl(KVList(k, v, appendKV(xs, ys)))
        } else if (k == xs.key) {
            kvlist_refl(KVList(k, v, appendKV(xs.tail, ys)))
        } else {
            sortedInsert_skipRight(xs.tail, ys, k, v)
            val lhsTail = sortedInsert(appendKV(xs.tail, ys), k, v)
            val rhsTail = appendKV(sortedInsert(xs.tail, k, v), ys)
            if (lhsTail != null) kvlist_refl(lhsTail)
            if (rhsTail != null) kvlist_refl(rhsTail)
        }
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>sortedInsert_skipLeft<!>(xs: KVList?, ys: KVList?, k: Int, v: Int): Unit {
    preconditions { kvAllLt(xs, k) }
    postconditions<Unit> {
        sortedInsert(appendKV(xs, ys), k, v) == appendKV(xs, sortedInsert(ys, k, v))
    }
    if (xs == null) {
        val r = sortedInsert(ys, k, v)
        if (r != null) kvlist_refl(r)
    } else {
        sortedInsert_skipLeft(xs.tail, ys, k, v)
        val lhsTail = sortedInsert(appendKV(xs.tail, ys), k, v)
        val rhsTail = appendKV(xs.tail, sortedInsert(ys, k, v))
        if (lhsTail != null) kvlist_refl(lhsTail)
        if (rhsTail != null) kvlist_refl(rhsTail)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>inOrderInsert<!>(t: PBST?, k: Int, v: Int): Unit {
    preconditions { isBST(t) }
    postconditions<Unit> {
        inOrder(insert(t, k, v)) == sortedInsert(inOrder(t), k, v)
    }
    if (t == null) {
        kvlist_refl(KVList(k, v, null))
    } else {
        val Y = KVList(t.key, t.value, inOrder(t.right))
        if (k < t.key) {
            inOrderInsert(t.left, k, v)
            allGt_inOrder(t.right, t.key)
            kvAllGt_relax(inOrder(t.right), t.key, k)
            sortedInsert_skipRight(inOrder(t.left), Y, k, v)
            appendKV_congL(
                inOrder(insert(t.left, k, v)),
                sortedInsert(inOrder(t.left), k, v),
                Y
            )
            kvlist_sym(
                sortedInsert(appendKV(inOrder(t.left), Y), k, v),
                appendKV(sortedInsert(inOrder(t.left), k, v), Y)
            )
            kvlist_trans(
                appendKV(inOrder(insert(t.left, k, v)), Y),
                appendKV(sortedInsert(inOrder(t.left), k, v), Y),
                sortedInsert(appendKV(inOrder(t.left), Y), k, v)
            )

            val lhsUnfolded = appendKV(inOrder(insert(t.left, k, v)), Y)
            val rhsUnfolded = sortedInsert(appendKV(inOrder(t.left), Y), k, v)
            if (lhsUnfolded != null) kvlist_refl(lhsUnfolded)
            if (rhsUnfolded != null) kvlist_refl(rhsUnfolded)

            val lhs = inOrder(insert(t, k, v))
            val rhs = sortedInsert(inOrder(t), k, v)
            if (lhs != null) kvlist_refl(lhs)
            if (rhs != null) kvlist_refl(rhs)

            val tNew = PBST(t.key, t.value, insert(t.left, k, v), t.right)
            tree_refl(tNew)
            verify(insert(t, k, v) == tNew)
            verify(inOrder(tNew) == lhsUnfolded)
            verify(lhs == lhsUnfolded)
            verify(rhs == rhsUnfolded)

            kvlist_sym(rhs, rhsUnfolded)

            kvlist_trans(lhs, lhsUnfolded, rhsUnfolded)
            kvlist_trans(lhs, rhsUnfolded, rhs)
        } else if (k > t.key) {
            inOrderInsert(t.right, k, v)
            allLt_inOrder(t.left, t.key)
            kvAllLt_relax(inOrder(t.left), t.key, k)
            sortedInsert_skipLeft(inOrder(t.left), Y, k, v)

            appendKV_congR(
                inOrder(t.left),
                KVList(t.key, t.value, inOrder(insert(t.right, k, v))),
                KVList(t.key, t.value, sortedInsert(inOrder(t.right), k, v))
            )

            val expectedSortedY = KVList(t.key, t.value, sortedInsert(inOrder(t.right), k, v))
            kvlist_refl(expectedSortedY)
            verify(sortedInsert(Y, k, v) == expectedSortedY)

            val lhsUnfolded = appendKV(
                inOrder(t.left),
                KVList(t.key, t.value, inOrder(insert(t.right, k, v)))
            )
            val midForm = appendKV(inOrder(t.left), expectedSortedY)
            val skipLeftRhs = appendKV(inOrder(t.left), sortedInsert(Y, k, v))
            val rhsUnfolded = sortedInsert(appendKV(inOrder(t.left), Y), k, v)

            if (lhsUnfolded != null) kvlist_refl(lhsUnfolded)
            if (midForm != null) kvlist_refl(midForm)
            if (skipLeftRhs != null) kvlist_refl(skipLeftRhs)
            if (rhsUnfolded != null) kvlist_refl(rhsUnfolded)

            verify(midForm == skipLeftRhs)

            kvlist_trans(lhsUnfolded, midForm, skipLeftRhs)
            kvlist_sym(rhsUnfolded, skipLeftRhs)
            kvlist_trans(lhsUnfolded, skipLeftRhs, rhsUnfolded)

            val lhs = inOrder(insert(t, k, v))
            val rhs = sortedInsert(inOrder(t), k, v)
            if (lhs != null) kvlist_refl(lhs)
            if (rhs != null) kvlist_refl(rhs)

            val tNew = PBST(t.key, t.value, t.left, insert(t.right, k, v))
            tree_refl(tNew)
            verify(insert(t, k, v) == tNew)
            verify(inOrder(tNew) == lhsUnfolded)
            verify(lhs == lhsUnfolded)
            verify(rhs == rhsUnfolded)

            kvlist_sym(rhs, rhsUnfolded)

            kvlist_trans(lhs, lhsUnfolded, rhsUnfolded)
            kvlist_trans(lhs, rhsUnfolded, rhs)
        } else {
            allLt_inOrder(t.left, t.key)
            sortedInsert_skipLeft(inOrder(t.left), Y, k, v)

            val expectedSortedY = KVList(k, v, inOrder(t.right))
            kvlist_refl(expectedSortedY)
            verify(sortedInsert(Y, k, v) == expectedSortedY)

            val lhsUnfolded = appendKV(inOrder(t.left), expectedSortedY)
            val skipLeftRhs = appendKV(inOrder(t.left), sortedInsert(Y, k, v))
            val rhsUnfolded = sortedInsert(appendKV(inOrder(t.left), Y), k, v)

            if (lhsUnfolded != null) kvlist_refl(lhsUnfolded)
            if (skipLeftRhs != null) kvlist_refl(skipLeftRhs)
            if (rhsUnfolded != null) kvlist_refl(rhsUnfolded)

            verify(lhsUnfolded == skipLeftRhs)

            kvlist_sym(rhsUnfolded, skipLeftRhs)
            kvlist_trans(lhsUnfolded, skipLeftRhs, rhsUnfolded)

            val lhs = inOrder(insert(t, k, v))
            val rhs = sortedInsert(inOrder(t), k, v)
            if (lhs != null) kvlist_refl(lhs)
            if (rhs != null) kvlist_refl(rhs)

            val tNew = PBST(k, v, t.left, t.right)
            tree_refl(tNew)
            verify(insert(t, k, v) == tNew)
            verify(inOrder(tNew) == lhsUnfolded)
            verify(lhs == lhsUnfolded)
            verify(rhs == rhsUnfolded)

            kvlist_sym(rhs, rhsUnfolded)

            kvlist_trans(lhs, lhsUnfolded, rhsUnfolded)
            kvlist_trans(lhs, rhsUnfolded, rhs)
        }
    }
}
