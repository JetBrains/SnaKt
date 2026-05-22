// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

fun borrowBoth(a: @Borrowed Any, b: @Borrowed Any) {}

fun consumeBoth(a: @Unique Any, b: @Unique Any) {}

fun shareBoth(a: Any, b: Any) {}

fun `pass shared twice to shareBoth`(a: Any) {
    shareBoth(a, <!UNIQUENESS_MISMATCH!>a<!>)
}

fun `pass borrowed twice to borrowBoth`(a: @Borrowed Any) {
    borrowBoth(a, <!UNIQUENESS_MISMATCH!>a<!>)
}

fun `pass unique twice to consumeBoth`(a: @Unique Any) {
    consumeBoth(a, <!UNIQUENESS_MISMATCH!>a<!>)
}
