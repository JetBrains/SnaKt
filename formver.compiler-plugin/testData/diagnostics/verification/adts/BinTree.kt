// FULL_JDK

package diagnostics.verification.adts

import org.jetbrains.kotlin.formver.plugin.*

<!ADT_VIOLATION, ADT_VIOLATION, ADT_VIOLATION!>@ADT
sealed interface BinTree<!>

@ADT
data <!ADT_VIOLATION, ADT_VIOLATION, ADT_VIOLATION!>object Leaf<!> : BinTree

<!ADT_VIOLATION, ADT_VIOLATION, ADT_VIOLATION!>@ADT
data class Node(val left: BinTree, val value: Int, val right: BinTree) : BinTree<!>

<!INTERNAL_ERROR!>@Pure
fun mirror(t: BinTree): BinTree = when (t) {
        is Leaf -> Leaf
        is Node -> Node(mirror(t.right), t.value, mirror(t.left))
    }<!>

<!ADT_VIOLATION!>@Pure
fun size(t: BinTree): Int = when (t) {
        is Leaf -> 0
        is Node -> 1 + size(t.left) + size(t.right)
    }<!>

<!ADT_VIOLATION!>@AlwaysVerify
fun treeRefl(t: BinTree): Unit {
    postconditions<Unit> { t == t }
    when (t) {
        is Leaf -> {}
        is Node -> {
            treeRefl(t.left)
            treeRefl(t.right)
        }
    }
}<!>

<!ADT_VIOLATION!>@AlwaysVerify
fun treeTrans(a: BinTree, b: BinTree, c: BinTree): Unit {
    preconditions {
        a == b
        b == c
    }
    postconditions<Unit> { a == c }
    when (a) {
        is Leaf -> {}
        is Node -> when (b) {
            is Leaf -> {}
            is Node -> when (c) {
                is Leaf -> {}
                is Node -> {
                    treeTrans(a.left, b.left, c.left)
                    treeTrans(a.right, b.right, c.right)
                }
            }
        }
    }
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun mirrorMirrorIsId(t: BinTree): Unit {
    postconditions<Unit> { mirror(mirror(t)) == t }
    when (t) {
        is Leaf -> {}
        is Node -> {
            mirrorMirrorIsId(t.left)
            mirrorMirrorIsId(t.right)
        }
    }
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun mirrorPreservesSize(t: BinTree): Unit {
    postconditions<Unit> { size(mirror(t)) == size(t) }
    when (t) {
        is Leaf -> {}
        is Node -> {
            mirrorPreservesSize(t.left)
            mirrorPreservesSize(t.right)
        }
    }
}<!>

// Congruence for Cons: if the tails are equal, so are the Cons cells.
<!INTERNAL_ERROR!>@AlwaysVerify
fun consEq(v: Int, a: LinkedList, b: LinkedList): Unit {
    preconditions { a == b }
    postconditions<Unit> { Cons(v, a) == Cons(v, b) }
}<!>

// One-step unfolding of reverse on a Cons cell.
<!INTERNAL_ERROR!>@AlwaysVerify
fun reverseCons(x: Int, xs: LinkedList): Unit {
    postconditions<Unit> {
        reverse(Cons(x, xs)) == append(reverse(xs), Cons(x, Nil))
    }
    listRefl(append(reverse(xs), Cons(x, Nil)))
}<!>

<!INTERNAL_ERROR!>@Pure
fun flatten(t: BinTree): LinkedList = when (t) {
        is Leaf -> Nil
        is Node -> append(flatten(t.left), Cons(t.value, flatten(t.right)))
    }<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun flattenMirrorIsReverse(t: BinTree): Unit {
    postconditions<Unit> {
        flatten(mirror(t)) == reverse(flatten(t))
    }
    when (t) {
        is Leaf -> {
            reverseNil()
            listSym(reverse(Nil), Nil)
        }
        // Transform both flatten(mirror(t)) and reverse(flatten(t)) into the same
        // normal form: append(reverse(flatten(t.right)), Cons(v, reverse(flatten(t.left)))),
        // then connect via listTrans. Each step is a single congruence or rewrite.
        is Node -> {
            flattenMirrorIsReverse(t.left)
            flattenMirrorIsReverse(t.right)

            // Rewrite flatten(mirror(t)) to the normal form.
            appendCongL(flatten(mirror(t.right)), reverse(flatten(t.right)), Cons(t.value, flatten(mirror(t.left))))
            consEq(t.value, flatten(mirror(t.left)), reverse(flatten(t.left)))
            appendCongR(reverse(flatten(t.right)), Cons(t.value, flatten(mirror(t.left))), Cons(t.value, reverse(flatten(t.left))))
            listRefl(flatten(mirror(t)))
            listTrans(flatten(mirror(t)), append(flatten(mirror(t.right)), Cons(t.value, flatten(mirror(t.left)))), append(reverse(flatten(t.right)), Cons(t.value, flatten(mirror(t.left)))))
            listTrans(flatten(mirror(t)), append(reverse(flatten(t.right)), Cons(t.value, flatten(mirror(t.left)))), append(reverse(flatten(t.right)), Cons(t.value, reverse(flatten(t.left)))))

            // Rewrite reverse(flatten(t)) to the same normal form.
            reverseAppend(flatten(t.left), Cons(t.value, flatten(t.right)))
            reverseCons(t.value, flatten(t.right))
            appendCongL(reverse(Cons(t.value, flatten(t.right))), append(reverse(flatten(t.right)), Cons(t.value, Nil)), reverse(flatten(t.left)))
            appendAssoc(reverse(flatten(t.right)), Cons(t.value, Nil), reverse(flatten(t.left)))
            appendSingletonCons(t.value, reverse(flatten(t.left)))
            appendCongR(reverse(flatten(t.right)), append(Cons(t.value, Nil), reverse(flatten(t.left))), Cons(t.value, reverse(flatten(t.left))))
            listRefl(reverse(flatten(t)))
            listTrans(reverse(flatten(t)), append(reverse(Cons(t.value, flatten(t.right))), reverse(flatten(t.left))), append(append(reverse(flatten(t.right)), Cons(t.value, Nil)), reverse(flatten(t.left))))
            listTrans(reverse(flatten(t)), append(append(reverse(flatten(t.right)), Cons(t.value, Nil)), reverse(flatten(t.left))), append(reverse(flatten(t.right)), append(Cons(t.value, Nil), reverse(flatten(t.left)))))
            listTrans(reverse(flatten(t)), append(reverse(flatten(t.right)), append(Cons(t.value, Nil), reverse(flatten(t.left)))), append(reverse(flatten(t.right)), Cons(t.value, reverse(flatten(t.left)))))

            // Both sides now equal the normal form, connect them.
            listSym(reverse(flatten(t)), append(reverse(flatten(t.right)), Cons(t.value, reverse(flatten(t.left)))))
            listTrans(flatten(mirror(t)), append(reverse(flatten(t.right)), Cons(t.value, reverse(flatten(t.left)))), reverse(flatten(t)))
        }
    }
}<!>


// LinkedList ADT Section
// We re-introduce definitions, functions and lemmas from LinkedListWithSumAdt.kt to use them in proofs

<!ADT_VIOLATION, ADT_VIOLATION, ADT_VIOLATION!>@ADT
sealed interface LinkedList<!>

@ADT
data <!ADT_VIOLATION, ADT_VIOLATION, ADT_VIOLATION!>object Nil<!> : LinkedList

<!ADT_VIOLATION, ADT_VIOLATION, ADT_VIOLATION!>@ADT
data class Cons(val head: Int, val tail: LinkedList) : LinkedList<!>

<!INTERNAL_ERROR!>@Pure
fun append(xs: LinkedList, ys: LinkedList): LinkedList = when (xs) {
        is Nil -> ys
        is Cons -> Cons(xs.head, append(xs.tail, ys))
    }<!>

<!INTERNAL_ERROR!>@Pure
fun reverse(xs: LinkedList): LinkedList = when (xs) {
        is Nil -> Nil
        is Cons -> append(reverse(xs.tail), Cons(xs.head, Nil))
    }<!>

<!ADT_VIOLATION!>@AlwaysVerify
fun listRefl(xs: LinkedList): Unit {
    postconditions<Unit> { xs == xs }
    when (xs) {
        is Nil -> {}
        is Cons -> listRefl(xs.tail)
    }
}<!>

<!ADT_VIOLATION!>@AlwaysVerify
fun listSym(a: LinkedList, b: LinkedList): Unit {
    preconditions { a == b }
    postconditions<Unit> { b == a }
    when (a) {
        is Nil -> {}
        is Cons -> when (b) {
            is Nil -> {}
            is Cons -> listSym(a.tail, b.tail)
        }
    }
}<!>

<!ADT_VIOLATION!>@AlwaysVerify
fun listTrans(a: LinkedList, b: LinkedList, c: LinkedList): Unit {
    preconditions {
        a == b
        b == c
    }
    postconditions<Unit> { a == c }
    when (a) {
        is Nil -> {}
        is Cons -> when (b) {
            is Nil -> {}
            is Cons -> when (c) {
                is Nil -> {}
                is Cons -> listTrans(a.tail, b.tail, c.tail)
            }
        }
    }
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun appendNull(xs: LinkedList): Unit {
    postconditions<Unit> { append(xs, Nil) == xs }
    when (xs) {
        is Nil -> {}
        is Cons -> appendNull(xs.tail)
    }
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun appendAssoc(xs: LinkedList, ys: LinkedList, zs: LinkedList): Unit {
    postconditions<Unit> {
        append(append(xs, ys), zs) == append(xs, append(ys, zs))
    }
    when (xs) {
        is Nil -> listRefl(append(ys, zs))
        is Cons -> appendAssoc(xs.tail, ys, zs)
    }
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun appendCongL(a: LinkedList, b: LinkedList, c: LinkedList): Unit {
    preconditions { a == b }
    postconditions<Unit> { append(a, c) == append(b, c) }
    when (a) {
        is Nil -> listRefl(c)
        is Cons -> when (b) {
            is Nil -> {}
            is Cons -> appendCongL(a.tail, b.tail, c)
        }
    }
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun appendCongR(a: LinkedList, b: LinkedList, c: LinkedList): Unit {
    preconditions { b == c }
    postconditions<Unit> { append(a, b) == append(a, c) }
    when (a) {
        is Nil -> listRefl(b)
        is Cons -> appendCongR(a.tail, b, c)
    }
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun appendNilL(xs: LinkedList): Unit {
    postconditions<Unit> { append(Nil, xs) == xs }
    listRefl(xs)
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun appendSingletonCons(x: Int, ys: LinkedList): Unit {
    postconditions<Unit> { append(Cons(x, Nil), ys) == Cons(x, ys) }
    appendNilL(ys)
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun reverseNil(): Unit {
    postconditions<Unit> { reverse(Nil) == Nil }
    listRefl(Nil)
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun reverseAppend(xs: LinkedList, ys: LinkedList): Unit {
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
            appendCongL(reverse(append(xs.tail, ys)), append(reverse(ys), reverse(xs.tail)), Cons(xs.head, Nil))
            appendAssoc(reverse(ys), reverse(xs.tail), Cons(xs.head, Nil))
            listRefl(reverse(append(xs, ys)))
            listTrans(reverse(append(xs, ys)), append(reverse(append(xs.tail, ys)), Cons(xs.head, Nil)), append(append(reverse(ys), reverse(xs.tail)), Cons(xs.head, Nil)))
            listTrans(reverse(append(xs, ys)), append(append(reverse(ys), reverse(xs.tail)), Cons(xs.head, Nil)), append(reverse(ys), append(reverse(xs.tail), Cons(xs.head, Nil))))
        }
    }
}<!>
