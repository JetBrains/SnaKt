// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A()

fun borrow(@Borrowed y: Any) {}

fun consume(@Unique y: Any) {}

fun share(y: Any) {}

// Borrowing locals

fun `borrow shared`(z: A) {
    borrow(z)
}

fun `borrow borrowed`(@Borrowed z: A) {
    borrow(z)
}

fun `borrow unique`(@Unique z: A) {
    borrow(z)
}

fun `borrow unique-borrowed`(@Borrowed @Unique z: A) {
    borrow(z)
}

// Borrowing local after consuming

fun `borrow after consuming unique`(@Unique a: A) {
    consume(a)
    borrow(<!UNIQUENESS_VIOLATION!>a<!>)
}

// Borrowing local after sharing

fun `borrow after sharing shared`(a: A) {
    share(a)
    borrow(a)
}

fun `borrow after sharing unique`(@Unique a: A) {
    share(a)
    borrow(a)
}
