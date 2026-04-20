// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Unique

class A(
    @Unique var um: A,  // unique-mutable
    @Unique val ui: A,  // unique-immutable
    var sm: A,          // shared-mutable
    val si: A,          // shared-immutable
)

fun consume(@Unique x: Any) {}

fun `consume nested unique after moving back`(@Unique a: A) {
    @Unique val b = a.um
    consume(b.um)
    a.um = b

    consume(<!UNIQUENESS_VIOLATION!>a<!>)
}
