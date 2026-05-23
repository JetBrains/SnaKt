// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A()

fun consume(a: @Unique Any) {}

fun borrow(a: @Borrowed Any) {}

fun borrowUnique(a: @Borrowed @Unique Any) {}

// Consuming local

fun `consume shared`(a: A) {
    consume(<!UNIQUENESS_MISMATCH!>a<!>)
}

fun `consume borrowed`(a: @Borrowed A) {
    consume(<!LOCALITY_MISMATCH, UNIQUENESS_MISMATCH!>a<!>)
}

fun `consume unique`(a: @Unique A) {
    consume(a)
}

fun `consume unique-borrowed`(a: @Unique @Borrowed A) {
    consume(<!LOCALITY_MISMATCH!>a<!>)
}

// Consuming local after borrowing

fun `consume shared after borrowing it`(a: A) {
    borrow(a)
    consume(<!UNIQUENESS_MISMATCH!>a<!>)
}

fun `consume borrowed after borrowing it`(a: @Borrowed A) {
    borrow(a)
    consume(<!LOCALITY_MISMATCH, UNIQUENESS_MISMATCH!>a<!>)
}

fun `consume after borrowing unique`(a: @Unique A) {
    borrow(a)
    consume(a)
}

fun `consume after borrowing unique as unique`(a: @Unique A) {
    borrowUnique(a)
    consume(a)
}

fun `consume after borrowing unique-borrowed`(a: @Unique @Borrowed A) {
    borrow(a)
    consume(<!LOCALITY_MISMATCH!>a<!>)
}

fun `consume after after borrowing unique-borrowed as unique`(a: @Unique @Borrowed A) {
    borrowUnique(a)
    consume(<!LOCALITY_MISMATCH!>a<!>)
}

fun `consume unique after storing type check`(a: @Unique Any) {
    val ok = a is A
    consume(a)
}

fun `consume unique after safe cast`(a: @Unique Any) {
    val cast = a as? A ?: return
    consume(cast)
    consume(<!UNIQUENESS_MISMATCH!>a<!>)
    consume(<!UNIQUENESS_MISMATCH!>cast<!>)
}
