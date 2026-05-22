// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    var x: @Unique Any = Any()
    var w: @Unique Any = Any()
}

class B {
    var y: @Unique A = A()
}

fun borrow(x: @Unique @Borrowed A) {}

fun consume(a: @Unique Any) {}

// Property assignments

fun `assign shared to unique subproperty`(x: @Unique B, v: A): Unit {
    x.y = v
}

fun `assign borrowed to unique subproperty`(x: @Unique B, v: @Borrowed A): Unit {
    x.y = <!LOCALITY_MISMATCH!>v<!>
}

fun `assign unique to unique subproperty`(x: @Unique B, v: @Unique A): Unit {
    x.y = v

    consume(x)
}

fun `assign unique-borrowed to unique subproperty`(x: @Unique B, v: @Unique @Borrowed A): Unit {
    x.y = <!LOCALITY_MISMATCH!>v<!>

    consume(x)
}
