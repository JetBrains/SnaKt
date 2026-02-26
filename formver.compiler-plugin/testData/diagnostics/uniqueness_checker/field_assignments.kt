// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.Borrowed

class A(
    @Unique var y: Any
)

fun f1(@Unique @Borrowed x: A, @Unique v: Any): A {
    x.y = v

    <!UNIQUENESS_VIOLATION!>return x<!>
}

fun borrowA(@Unique @Borrowed x: A) {
}

fun f2(@Unique x: A, v: Any) {
    x.y = v
    borrowA(<!UNIQUENESS_VIOLATION!>x<!>)
}

fun consumeA(@Unique a: A) {}

fun f3(@Unique x: A, @Unique v: Any) {
    x.y = v
    consumeA(x)
}

fun f4(@Unique @Borrowed x: A, v: Any) {
    x.y = v
    borrowA(<!UNIQUENESS_VIOLATION!>x<!>)
}

fun f5(@Unique x: A, @Unique v: Any): Any {
    x.y = v

    return x.y
}

fun f6(@Unique x: A, @Unique v1: Any, @Unique v2: Any): A {
    x.y = v1
    x.y = v2

    return x
}

fun f7(@Unique x: A, @Unique v: Any) {
    x.y = v
    consumeAny(x.y)
}

fun consumeAny(@Unique a: Any) {}

fun f8(@Unique @Borrowed x: A) {
    consumeAny(<!UNIQUENESS_VIOLATION!>x.y<!>)
}

fun shareAny(a: Any) {}

fun f9(@Unique x: A, v: Any) {
    x.y = v
    shareAny(x.y)
}

fun f10(@Unique @Borrowed x: A, @Unique v: Any) {
    x.y = v
    borrowA(x)
    borrowA(x)
}

fun f11(x: A) {
    consumeAny(<!UNIQUENESS_VIOLATION!>x.y<!>)
}