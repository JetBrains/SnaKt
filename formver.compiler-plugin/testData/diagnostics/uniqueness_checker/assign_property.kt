// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    @Unique var x = Any()
    @Unique var w = Any()
}

class B {
    @Unique var y = A()
}

fun borrow(@Unique @Borrowed x: A) {}

fun consume(@Unique a: Any) {}

// Property assignments

fun `assign shared to unique subproperty`(@Unique x: B, v: A): Unit {
    x.y = <!UNIQUENESS_VIOLATION!>v<!>
}

fun `assign borrowed to unique subproperty`(@Unique x: B, @Borrowed v: A): Unit {
    x.y = <!UNIQUENESS_VIOLATION!>v<!>
}

fun `assign unique to unique subproperty`(@Unique x: B, @Unique v: A): Unit {
    x.y = v

    consume(x)
}

fun `assign unique-borrowed to unique subproperty`(@Unique x: B, @Unique @Borrowed v: A): Unit {
    x.y = <!UNIQUENESS_VIOLATION!>v<!>

    consume(x)
}
