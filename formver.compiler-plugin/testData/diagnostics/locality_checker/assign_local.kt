// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `assign global to local`(x: Any) {
    @Borrowed var y: Any

    y = x
}

fun `assign local to global`(@Borrowed x: Any) {
    var y: Any

    y = <!LOCALITY_VIOLATION!>x<!>
}

fun `assign local to local`(@Borrowed x: Any) {
    @Borrowed var y: Any

    y = x
}

fun `assign local then global to local`(@Borrowed x: Any, y: Any) {
    @Borrowed var z: Any

    z = x
    z = y
}

fun `assign local to local in loop`(@Borrowed x: Any) {
    @Borrowed var z: Any

    while (true) {
        z = x
    }
}
