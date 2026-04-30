// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

class A()

fun borrow(x: @Borrowed A) {}

fun `resolve captured local owner from nested lambda`(x: @Borrowed A) {
    val x: @Borrowed A = x

    run {
        borrow(<!LOCALITY_VIOLATION!>x<!>)
    }

    borrow(x)
}

fun `resolve shadowing local's owner first`(x: @Borrowed A) {
    val x: @Borrowed A = x

    run {
        val x: @Borrowed A = A()
        borrow(x)
    }

    borrow(x)
}
