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

fun borrow(y: @Borrowed Any) {}

fun consume(y: @Unique Any) {}

fun share(a: Any) {}

// Borrowing subproperties

fun `borrow shared subproperty`(z: B) {
    borrow(z.y)
}

fun `borrow borrowed subproperty`(z: @Borrowed B) {
    borrow(z.y)
}

fun `borrow unique subproperty`(z: @Unique B) {
    borrow(z.y)
}

fun `borrow unique-borrowed subproperty`(z: @Borrowed @Unique B) {
    borrow(z.y)
}

fun `borrow multiple unique subproperties`(z: @Unique B) {
    borrow(z.y.x)
    borrow(z.y.w)
}

// Borrowing partially-inconsistent subproperties

fun `borrow partially moved`(z: @Unique B) {
    consume(z.y)
    borrow(<!UNIQUENESS_MISMATCH!>z<!>)
}

fun `borrow partially shared`(z: @Unique B) {
    share(z.y)
    borrow(z)
}

// Borrowing after assignment

fun `borrow unique parent after assigning subproperty to unique`(x: @Unique B, v: @Unique A) {
    x.y = v
    borrow(x)
}

fun `borrow unique parent twice after assigning subproperty to unique`(x: @Unique @Borrowed B, v: @Unique A) {
    x.y = v
    borrow(x)
    borrow(x)
}
