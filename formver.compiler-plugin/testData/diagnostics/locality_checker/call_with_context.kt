// LOCALITY_CHECK_ONLY
// LANGUAGE: +ContextParameters

import org.jetbrains.kotlin.formver.plugin.Borrowed

class A

context(_: A)
fun requireGlobalContext() {}

context(_: @Borrowed A)
fun requireLocalContext() {}

val A.globalExtensionProperty: Any
    get() = Any()

context(_: A)
val globalContextProperty: Any
    get() = Any()

context(_: @Borrowed A)
val localContextProperty: Any
    get() = Any()

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

fun `pass local as shared extension property receiver`() {
    val x: @Borrowed A = A()
    val y = <!LOCALITY_VIOLATION!>x<!>.globalExtensionProperty
}

context(_: @Borrowed A)
fun `pass local as implicit global property context argument`() {
    val x = <!LOCALITY_VIOLATION!>globalContextProperty<!>
}

context(_: @Borrowed A)
fun `pass local as implicit local property context argument`() {
    val x = localContextProperty
}
