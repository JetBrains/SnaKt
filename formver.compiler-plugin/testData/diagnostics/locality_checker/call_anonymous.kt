// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `call local_() lambda with local target`(x: @Borrowed Any, f: (@Borrowed Any).() -> Unit) {
    x.f()
}

fun `call local_() lambda with global target`(x: Any, f: (@Borrowed Any).() -> Unit) {
    x.f()
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
