// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A()

fun consume(@Unique a: Any) {}

fun borrow(@Borrowed a: Any) {}

fun borrowUnique(@Borrowed @Unique a: Any) {}

// Consuming local

fun `consume shared`(a: A) {
    consume(<!UNIQUENESS_VIOLATION!>a<!>)
}

fun `consume borrowed`(@Borrowed a: A) {
    consume(<!UNIQUENESS_VIOLATION!>a<!>)
}

fun `consume unique`(@Unique a: A) {
    consume(a)
}

fun `consume unique-borrowed`(@Unique @Borrowed a: A) {
    consume(<!UNIQUENESS_VIOLATION!>a<!>)
}

// Consuming local after borrowing

fun `consume shared after borrowing it`(a: A) {
    borrow(a)
    consume(<!UNIQUENESS_VIOLATION!>a<!>)
}

fun `consume borrowed after borrowing it`(@Borrowed a: A) {
    borrow(a)
    consume(<!UNIQUENESS_VIOLATION!>a<!>)
}

fun `consume after borrowing unique`(@Unique a: A) {
    borrow(a)
    consume(a)
}

fun `consume after borrowing unique as unique`(@Unique a: A) {
    borrowUnique(a)
    consume(a)
}

fun `consume after borrowing unique-borrowed `(@Unique @Borrowed a: A) {
    borrow(a)
    consume(<!UNIQUENESS_VIOLATION!>a<!>)
}

fun `consume after after borrowing unique-borrowed as unique`(@Unique @Borrowed a: A) {
    borrowUnique(a)
    consume(<!UNIQUENESS_VIOLATION!>a<!>)
}

fun `consume unique after storing type check`(@Unique a: Any) {
    val ok = a is A
    consume(a)
}
