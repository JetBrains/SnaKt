// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A(
    var x: @Unique Any,
    var w: @Unique Throwable
)

fun `return unique field of a local shared`(x: @Borrowed A): Any {
    <!INVALID_LEAKED_UNIQUENESS!>return x.x<!>
}

fun `return unique field of a local unique`(x: @Borrowed @Unique A): @Unique Any {
    <!INVALID_LEAKED_UNIQUENESS!>return x.x<!>
}

fun `throw unique field of a local shared`(x: @Borrowed A): Any {
    <!INVALID_LEAKED_UNIQUENESS!>throw x.w<!>
}

fun `throw unique field of a local unique`(x: @Borrowed @Unique A): @Unique Any {
    <!INVALID_LEAKED_UNIQUENESS!>throw x.w<!>
}
