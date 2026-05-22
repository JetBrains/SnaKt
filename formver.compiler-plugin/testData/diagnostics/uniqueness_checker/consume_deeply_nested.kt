// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Unique

class A(
    var um: @Unique A,  // unique-mutable
    val ui: @Unique A,  // unique-immutable
    var sm: A,          // shared-mutable
    val si: A,          // shared-immutable
)

fun consume(x: @Unique Any) {}

fun `consume nested unique after moving back`(a: @Unique A) {
    val b: @Unique A = a.um
    consume(b.um)
    a.um = <!UNIQUENESS_MISMATCH!>b<!>

    consume(a)
}
