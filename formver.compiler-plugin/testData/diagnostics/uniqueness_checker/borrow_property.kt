// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    @Unique var x = Any()
    @Unique var w = Any()
}

class B {
    @Unique var y = A()
}

fun borrow(@Borrowed y: Any) {}

fun consume(@Unique y: Any) {}

fun share(a: Any) {}

// Borrowing subproperties

fun `borrow shared subproperty`(z: B) {
    borrow(z.y)
}

fun `borrow borrowed subproperty`(@Borrowed z: B) {
    borrow(z.y)
}

fun `borrow unique subproperty`(@Unique z: B) {
    borrow(z.y)
}

fun `borrow unique-borrowed subproperty`(@Borrowed @Unique z: B) {
    borrow(z.y)
}

fun `borrow multiple unique subproperties`(@Unique z: B) {
    borrow(z.y.x)
    borrow(z.y.w)
}

// Borrowing partially-inconsistent subproperties

fun `borrow partially moved`(@Unique z: B) {
    consume(z.y)
    borrow(<!UNIQUENESS_VIOLATION!>z<!>)
}

fun `borrow partially shared`(@Unique z: B) {
    share(z.y)
    borrow(<!UNIQUENESS_VIOLATION!>z<!>)
}

// Borrowing after assignment

fun `borrow unique parent after assigning subproperty to unique`(@Unique x: B, @Unique v: A) {
    x.y = v
    borrow(x)
}

fun `borrow unique parent twice after assigning subproperty to unique`(@Unique @Borrowed x: B, @Unique v: A) {
    x.y = v
    borrow(x)
    borrow(x)
}
