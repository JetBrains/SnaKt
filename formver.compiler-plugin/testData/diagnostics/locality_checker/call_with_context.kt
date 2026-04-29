// LOCALITY_CHECK_ONLY
// LANGUAGE: +ContextParameters

import org.jetbrains.kotlin.formver.plugin.Borrowed

class A

class B

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

context(_: A)
fun requireGlobalA() {}

context(_: @Borrowed A)
fun requireLocalA() {}

context(_: A, _: B)
fun requireGlobalAAndB() {}

context(_: @Borrowed A, _: B)
fun requireLocalAAndGlobalB() {}

context(_: A)
val globalContextPropertyA: Any
    get() = Any()

context(_: A, _: B)
val globalContextPropertyAB: Any
    get() = Any()

fun `pass local with 'with' as global context argument`(x: @Borrowed A) {
    <!INVALID_LOCALITY_TYPE_TARGET!>with<!>(<!LOCALITY_VIOLATION!>x<!>) {
        <!LOCALITY_VIOLATION!>requireGlobalA()<!>
    }
}

fun `pass local with 'with' as local context argument`(x: @Borrowed A) {
    <!INVALID_LOCALITY_TYPE_TARGET!>with<!>(<!LOCALITY_VIOLATION!>x<!>) {
        requireLocalA()
    }
}

context(_: @Borrowed A, _: B)
fun `pass local as first of mixed context arguments`() {
    <!LOCALITY_VIOLATION!>requireGlobalAAndB()<!>
}

context(_: @Borrowed A, _: B)
fun `pass local and global as mixed context arguments`() {
    requireLocalAAndGlobalB()
}

context(_: @Borrowed A, _: B)
fun `pass local as first mixed property context argument`() {
    val x = <!LOCALITY_VIOLATION!>globalContextPropertyAB<!>
}

fun `pass local with with as global property context argument`(x: @Borrowed A) {
    <!INVALID_LOCALITY_TYPE_TARGET!>with<!>(<!LOCALITY_VIOLATION!>x<!>) {
        val y = <!LOCALITY_VIOLATION!>globalContextPropertyA<!>
    }
}
