// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

fun `return shared`(a: Throwable) {
    throw a
}

fun `return borrowed`(@Borrowed a: Throwable) {
    <!UNIQUENESS_VIOLATION!>throw a<!>
}

fun `return unique`(@Unique a: Throwable) {
    throw a
}

fun `return unique-borrowed`(@Unique @Borrowed a: Throwable) {
    <!UNIQUENESS_VIOLATION!>throw a<!>
}
