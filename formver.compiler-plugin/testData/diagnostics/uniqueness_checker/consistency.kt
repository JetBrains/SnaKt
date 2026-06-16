// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    var x: @Unique Any = Any()
    var w: @Unique Any = Any()
}

class B {
    var y: @Unique A = A()
}

class C {
    var b: @Unique B = B()
}

class D {
    var c: @Unique C = C()
}

fun consume(a: @Unique Any) {}

fun borrow(a: @Borrowed Any) {}

fun share(a: Any) {}

fun nondet(): Boolean = false

fun `consume grandchild leaks grandparent`(d: @Unique D) {
    consume(d.c.b.y)

    consume(<!LEAKED_UNIQUENESS_CONSISTENCY_VIOLATION!>d<!>)
}

fun `consume two siblings leaks parent`(b: @Unique B) {
    consume(b.y.x)
    consume(b.y.w)

    consume(<!LEAKED_UNIQUENESS_CONSISTENCY_VIOLATION, LEAKED_UNIQUENESS_CONSISTENCY_VIOLATION!>b<!>)
}

fun `consume then reassign sub-property does not leak`(b: @Unique B, fresh: @Unique A) {
    consume(b.y)
    b.y = fresh

    consume(b)
}

fun `consume one of two siblings then borrow parent`(b: @Unique B) {
    consume(b.y.x)
    borrow(<!LEAKED_UNIQUENESS_CONSISTENCY_VIOLATION!>b<!>)
}

fun `return parent after consuming child`(b: @Unique B): @Unique B {
    consume(b.y.x)

    return <!LEAKED_UNIQUENESS_CONSISTENCY_VIOLATION!>b<!>
}

fun `consume in if then use parent`(b: @Unique B) {
    if (nondet()) {
        consume(b.y)
    }

    consume(<!LEAKED_UNIQUENESS_CONSISTENCY_VIOLATION!>b<!>)
}

fun `borrow parent leaves children unique`(b: @Unique B) {
    borrow(b)
    consume(b.y)
}

fun `consume parent then access child`(b: @Unique B) {
    consume(b)
    // TODO: Verify whether b.y should be unique here (even though b is moved).
    consume(<!UNIQUENESS_MISMATCH!><!INVALID_MOVED_ACCESS!>b<!>.y<!>)
}

fun `move child via assignment then access parent`(b: @Unique B) {
    val local: @Unique A = b.y
    consume(local)

    consume(<!LEAKED_UNIQUENESS_CONSISTENCY_VIOLATION!>b<!>)
}

fun `move child via assignment then reassign`(b: @Unique B, fresh: @Unique A) {
    val local: @Unique A = b.y
    b.y = fresh
    consume(local)

    consume(b)
}
