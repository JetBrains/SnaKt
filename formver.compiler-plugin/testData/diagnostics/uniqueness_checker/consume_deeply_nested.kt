// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Unique

class A(
    var um: @Unique A,  // unique-mutable
    val ui: @Unique A,  // unique-immutable
    var sm: A,          // shared-mutable
    val si: A,          // shared-immutable
)

class B()

fun consume(x: @Unique Any) {}

fun `consume nested unique after moving back`(a: @Unique A) {
    val b: @Unique A = a.um
    consume(b.um)
    // Problem a.um is Moved and b is Moved, hence this pass while it shouldn't
    a.um = <!UNIQUENESS_MISMATCH!>b<!>

    consume(a)
}

fun test(a: @Unique A) {
    val x: @Unique A = a.um
    a.um = x
    consume(a)
}
