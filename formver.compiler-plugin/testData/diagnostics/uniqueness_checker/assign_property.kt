// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    @Unique var x = Object()
    @Unique var w = Object()
}

class B {
    @Unique var y = A()
}

fun borrow(@Unique @Borrowed x: A) {}

fun consume(@Unique a: Any) {}

// Property assignments

fun `assign shared to unique subproperty`(@Unique x: B, v: A): Unit {
    x.y = v

    consume(<!UNIQUENESS_VIOLATION!>x<!>)
}

fun `assign borrowed to unique subproperty`(@Unique x: B, @Borrowed v: A): Unit {
    x.y = v // TODO: Either disallow assigning borrowed to properties, or make sure that assigned properties cannot leak

    consume(<!UNIQUENESS_VIOLATION!>x<!>)
}

fun `assign unique to unique subproperty`(@Unique x: B, @Unique v: A): Unit {
    x.y = v // TODO: Either disallow assigning borrowed to properties, or make sure that assigned properties cannot leak

    consume(x)
}

fun `assign unique-borrowed to unique subproperty`(@Unique x: B, @Unique @Borrowed v: A): Unit {
    x.y = v

    consume(x)
}
