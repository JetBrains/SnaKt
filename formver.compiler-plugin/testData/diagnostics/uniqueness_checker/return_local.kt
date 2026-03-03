// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

fun consume(@Unique a: Any) {}

fun share(a: Any) {}

fun `return shared`(a: Any): Any {
    return a
}

fun `return borrowed`(@Borrowed a: Any): Any {
    <!UNIQUENESS_VIOLATION!>return a<!>
}

fun `return unique`(@Unique a: Any): Any {
    return a
}

fun `return unique-borrowed`(@Unique @Borrowed a: Any): Any {
    <!UNIQUENESS_VIOLATION!>return a<!>
}
