// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

fun `throw shared`(a: Throwable) {
    throw a
}

fun `throw borrowed`(@Borrowed a: Throwable) {
    throw <!UNIQUENESS_VIOLATION!>a<!>
}

fun `throw unique`(@Unique a: Throwable) {
    throw a
}

fun `throw unique-borrowed`(@Unique @Borrowed a: Throwable) {
    throw <!UNIQUENESS_VIOLATION!>a<!>
}
