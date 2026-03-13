@file:Suppress("USELESS_IS_CHECK")

// ALWAYS_VALIDATE
// Probe: do permissions work correctly across casts?
// Key concern: when we cast Foo->Bar, we inhale Bar$shared
// independently of Foo$shared. Since Bar$shared nests Foo$shared,
// we now have TWO independent copies of Foo's shared predicate.
// Is this sound?

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

open class Parent(val x: Int)
class Child(x: Int, val y: Int) : Parent(x)

// After unsafe cast, can we access both parent and child fields?
fun <!VIPER_TEXT!>castThenAccessBothFields<!>(p: Parent): Int {
    val c = p as Child
    return c.x + c.y
}

// After smart cast, can we access parent field?
fun <!VIPER_TEXT!>smartCastAccessParentField<!>(p: Parent): Int {
    if (p is Child) {
        return p.x  // should still work via parent's predicate
    }
    return 0
}

// After smart cast, can we access child field?
fun <!VIPER_TEXT!>smartCastAccessChildField<!>(p: Parent): Int {
    if (p is Child) {
        return p.y  // needs child's predicate
    }
    return 0
}

// After safe cast, access both fields
fun <!VIPER_TEXT!>safeCastAccessFields<!>(p: Parent): Int {
    val c = p as? Child
    return if (c != null) c.x + c.y else p.x
}

// After smart cast, can we verify a property about the parent field?
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>smartCastPreservesParentInfo<!>(p: Parent): Boolean {
    contract {
        returns(true)
    }
    if (p is Child) {
        return p is Parent  // trivially true, tests subtype axiom
    }
    return true
}
