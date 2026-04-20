// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `return shared value from expression body`(x: Any): Any = x

fun `return local value explicitly`(@Borrowed x: Any): Any {
    return <!LOCALITY_VIOLATION!>x<!>
}

fun `return local value from expression body`(@Borrowed x: Any): Any =
    <!LOCALITY_VIOLATION!>x<!>

fun `return local value from lambda body`(@Borrowed x: Any) {
    run<Any> {
        <!LOCALITY_VIOLATION!>x<!>
    }
}

class A(
    val x: Any
)

fun @receiver:Borrowed A.`return local receiver`(): A {
    return <!LOCALITY_VIOLATION!>this<!>
}

fun @receiver:Borrowed A.`return explicit local receiver property`(): Any {
    return this.x
}

fun @receiver:Borrowed A.`return implicit local receiver property`(): Any {
    return x
}
