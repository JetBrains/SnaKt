// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A(
    @Unique var um: B,  // unique-mutable
    @Unique val ui: B,  // unique-immutable
    var sm: B,          // shared-mutable
    val si: B,          // shared-immutable
)


class B(
    @Unique var um: C,  // unique-mutable
    @Unique val ui: C,  // unique-immutable
    var sm: C,          // shared-mutable
    val si: C,          // shared-immutable
)


class C(
    @Unique var um: Any,  // unique-mutable
    @Unique val ui: Any,  // unique-immutable
    var sm: Any,          // shared-mutable
    val si: Any,          // shared-immutable
)

fun consume(@Unique x: Any) {}

fun `consume nested unique after moving back`(@Unique a: A) {
    @Unique val b = a.um
    consume(b.um)
    a.um = b

    consume(a)
}