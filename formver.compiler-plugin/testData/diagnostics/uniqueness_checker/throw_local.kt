// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

fun `throw shared`(a: Throwable) {
    throw a
}

fun `throw borrowed`(a: @Borrowed Throwable) {
    throw <!LOCALITY_MISMATCH!>a<!>
}

fun `throw unique`(a: @Unique Throwable) {
    throw a
}

fun `throw unique-borrowed`(a: @Unique @Borrowed Throwable) {
    throw <!LOCALITY_MISMATCH!>a<!>
}
