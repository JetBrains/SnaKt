// LOCALITY_CHECK_ONLY
// LANGUAGE: +ContextParameters

import org.jetbrains.kotlin.formver.plugin.Borrowed

class A

fun requireLocalContextFunction(f: context(@Borrowed A) () -> Unit) {}

fun requireGlobalContextFunction(f: context(A) () -> Unit) {}

fun requireLocalContextAndArgumentFunction(f: context(@Borrowed A) (@Borrowed Any) -> Unit) {}

context(_: (@Borrowed Any) -> Unit)
fun requireLocalFunctionContext() {}

fun `pass global context function as local context function`(
    f: context(A) () -> Unit
) {
    requireLocalContextFunction(<!LOCALITY_CONTRACT_MISMATCH!>f<!>)
}

fun `pass local context function as global context function`(
    f: context(@Borrowed A) () -> Unit
) {
    requireGlobalContextFunction(f)
}

fun `assign global context function to local context function`(
    f: context(A) () -> Unit
) {
    val g: context(@Borrowed A) () -> Unit = <!LOCALITY_CONTRACT_MISMATCH!>f<!>
}

fun `pass global-context local-argument function as local-context local-argument function`(
    f: context(A) (@Borrowed Any) -> Unit
) {
    requireLocalContextAndArgumentFunction(<!LOCALITY_CONTRACT_MISMATCH!>f<!>)
}

context(_: (Any) -> Unit)
fun `pass global function as implicit local function context argument`() {
    <!CONTEXT_LOCALITY_CONTRACT_MISMATCH!>requireLocalFunctionContext()<!>
}
