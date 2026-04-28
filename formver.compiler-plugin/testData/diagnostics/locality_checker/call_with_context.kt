// LOCALITY_CHECK_ONLY
// LANGUAGE: +ContextParameters

import org.jetbrains.kotlin.formver.plugin.Borrowed

class A

context(_: A)
fun requireGlobalContext() {}

context(_: @Borrowed A)
fun requireLocalContext() {}

context(_: @Borrowed A)
fun `pass local as implicit global context argument`() {
    <!LOCALITY_VIOLATION!>requireGlobalContext()<!>
}

context(_: @Borrowed A)
fun `pass local as implicit local context argument`() {
    requireLocalContext()
}

context(_: A)
fun `pass global as implicit global context argument`() {
    requireGlobalContext()
}
