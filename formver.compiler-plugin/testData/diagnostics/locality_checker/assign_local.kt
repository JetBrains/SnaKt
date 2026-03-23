// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `assign global`(x: Any) {
    var y: @Borrowed Any

    y = <!LOCALITY_VIOLATION!>x<!>
}

fun `assign local`(x: @Borrowed Any) {
    var y: @Borrowed Any

    y = x
}

fun `assign local then global`(x: @Borrowed Any, y: Any) {
    var z: @Borrowed Any

    z = x
    z = <!LOCALITY_VIOLATION!>y<!>
}

fun `assign local in loop`(x: @Borrowed Any) {
    var z: @Borrowed Any

    while (true) {
        z = x
    }
}
