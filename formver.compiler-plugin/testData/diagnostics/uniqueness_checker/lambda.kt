// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A()

fun consume(a: @Unique Any) {}

fun borrow(a: @Borrowed Any) {}

fun share(a: Any) {}

fun callBorrowed(f: @Borrowed (() -> Unit)) {
    f()
}

// Lambda captures of unique values.

fun `lambda captures and consumes unique`(a: @Unique A) {
    val f: () -> Unit = { consume(<!UNIQUENESS_MISMATCH!>a<!>) }
    consume(a)
}

fun `lambda captures unique, invoked`(a: @Unique A) {
    val f: () -> Unit = { consume(<!UNIQUENESS_MISMATCH!>a<!>) }
    f()
    consume(a)
}

fun `lambda is borrowed argument`(a: @Unique A) {
    // TODO: Determine whether `a` should be moved inside this lambda.
    callBorrowed { borrow(a) }
    consume(a)
}

fun `local function consumes unique`(a: @Unique A) {
    fun consumeLocally() {
        consume(<!UNIQUENESS_MISMATCH!>a<!>)
    }

    consumeLocally()
    consume(a)
}

fun `local function does not run before use`(a: @Unique A) {
    fun consumeLocally() {
        consume(<!UNIQUENESS_MISMATCH!>a<!>)
    }

    consume(a)
}

fun runWith(a: @Unique Any, block: (@Unique Any) -> Unit) {
    block(a)
}

fun `pass unique through lambda parameter`(a: @Unique A) {
    runWith(a) { x -> consume(<!UNIQUENESS_MISMATCH!>x<!>) }
    consume(<!UNIQUENESS_MISMATCH!>a<!>)
}

fun produceUnique(producer: () -> @Unique A): @Unique A = producer()

fun `lambda returns unique`(a: @Unique A): @Unique A {
    val r = produceUnique { a }
    consume(<!UNIQUENESS_MISMATCH!>a<!>)
    return r
}
