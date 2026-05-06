// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `call local_() lambda with local target`(x: @Borrowed Any, f: (@Borrowed Any).() -> Unit) {
    x.f()
}

fun `call local_() lambda with global target`(x: Any, f: (@Borrowed Any).() -> Unit) {
    x.f()
}

fun `call global_() lambda with local target`(x: @Borrowed Any, f: (Any).() -> Unit) {
    <!LOCALITY_VIOLATION!>x<!>.f()
}

fun `call (global) lambda with global argument`(x: Any, f: (Any) -> Unit) {
    f(x)
}

fun `call (local) lambda with local argument`(x: @Borrowed Any, f: (@Borrowed Any) -> Unit) {
    f(x)
}

fun `call (global) lambda with local argument`(x: @Borrowed Any, f: (Any) -> Unit) {
    f(<!LOCALITY_VIOLATION!>x<!>)
}

fun `call ambiguous lambda with local argument`(x: @Borrowed Any, f: (Any) -> Unit, g: (@Borrowed Any) -> Unit) {
    (if (true) { f } else { g })(<!LOCALITY_VIOLATION!>x<!>)
}

fun `call unified-local lambda with local argument`(x: @Borrowed Any, f: (@Borrowed Any) -> Unit, g: (@Borrowed Any) -> Unit) {
    (if (true) { f } else { g })(x)
}
