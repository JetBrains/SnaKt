// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `throw shared value`(x: Throwable) {
    throw x
}

fun `throw local value explicitly`(x: @Borrowed Throwable) {
    throw <!LOCALITY_MISMATCH!>x<!>
}

fun @Borrowed Throwable.`throw local receiver explicitly`() {
    throw <!LOCALITY_MISMATCH!>this<!>
}

fun `throw local value from lambda`(x: @Borrowed Throwable) {
    run {
        throw <!INVALID_LOCALITY_CAPTURE, LOCALITY_MISMATCH!>x<!>
    }
}
