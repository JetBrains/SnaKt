// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A()

fun borrow(y: @Borrowed Any) {}

fun consume(y: @Unique Any) {}

fun share(y: Any) {}

// Borrowing locals

fun `borrow shared`(z: A) {
    borrow(z)
}

fun `borrow borrowed`(z: @Borrowed A) {
    borrow(z)
}

fun `borrow unique`(z: @Unique A) {
    borrow(z)
}

fun `borrow unique-borrowed`(z: @Borrowed @Unique A) {
    borrow(z)
}

// Borrowing local after consuming

fun `borrow after consuming unique`(a: @Unique A) {
    consume(a)
    borrow(<!UNIQUENESS_MISMATCH!>a<!>)
}

// Borrowing local after sharing

fun `borrow after sharing shared`(a: A) {
    share(a)
    borrow(<!UNIQUENESS_MISMATCH!>a<!>)
}

fun `borrow after sharing unique`(a: @Unique A) {
    share(a)
    borrow(<!UNIQUENESS_MISMATCH!>a<!>)
}
