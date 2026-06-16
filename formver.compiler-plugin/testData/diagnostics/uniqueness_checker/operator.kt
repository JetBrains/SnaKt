// FULL_JDK
// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    operator fun plus(other: @Unique A): @Unique A = A()
    operator fun get(index: Int): @Unique A = A()
    operator fun set(index: Int, value: @Unique A) {}
    operator fun unaryMinus(): @Unique A = A()
    operator fun contains(other: @Unique A): Boolean = false
}

fun consume(a: @Unique Any) {}

fun borrow(a: @Borrowed Any) {}

// Binary `+` desugars to `a.plus(b)`. Both `a` (dispatch receiver) and `b` (argument) are
// consumed by the analyzer (the receiver via its QualifiedAccessNode move; the argument
// via visitFunctionCallEnterNode). The initializer mismatch arises because
// `TerminalUniquenessResolver` returns `Shared` for any non-constructor `FirFunctionCall`,
// so the result of `a + b` is treated as Shared regardless of `plus`'s @Unique return type.

fun `consume after operator plus`(a: @Unique A, b: @Unique A) {
    // TODO: The mismatch on the right-hand side documents the gap -- a function call's
    // declared @Unique return type is not honored when resolving the call's uniqueness.
    val c: @Unique A = a + b
    consume(<!UNIQUENESS_MISMATCH!>a<!>)
    consume(<!UNIQUENESS_MISMATCH!>b<!>)
    consume(c)
}

// Indexed get/set

fun `consume after indexed get`(a: @Unique A, i: Int) {
    val x: @Unique A = a[i]
    consume(<!UNIQUENESS_MISMATCH!>a<!>)
    consume(x)
}

fun `consume after indexed set`(a: @Unique A, i: Int, v: @Unique A) {
    a[i] = v
    consume(<!UNIQUENESS_MISMATCH!>v<!>)
    consume(<!UNIQUENESS_MISMATCH!>a<!>)
}

// Unary operator

fun `consume after unary minus`(a: @Unique A) {
    val negA: @Unique A = -a
    consume(<!UNIQUENESS_MISMATCH!>a<!>)
    consume(negA)
}

// `in` operator (calls `contains`)

fun `consume after in operator`(a: @Unique A, b: @Unique A) {
    val isIn: Boolean = a in b
    consume(<!UNIQUENESS_MISMATCH!>a<!>)
    consume(<!UNIQUENESS_MISMATCH!>b<!>)
}

// String concatenation via `+`

fun `consume after string plus`(a: @Unique String) {
    val s: String = a + " suffix"
    consume(<!UNIQUENESS_MISMATCH!>a<!>)
}

// Augmented assignment += desugars to `a = a + b`

fun `consume after plus assign`(b: @Unique A) {
    var a: @Unique A = A()
    a = a + b
    consume(<!UNIQUENESS_MISMATCH!>b<!>)
    consume(a)
}
