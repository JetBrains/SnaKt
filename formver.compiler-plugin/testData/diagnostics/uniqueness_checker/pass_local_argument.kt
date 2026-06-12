// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

fun borrowBoth(a: @Borrowed Any, b: @Borrowed Any) {}

fun consumeBoth(a: @Unique Any, b: @Unique Any) {}

fun shareBoth(a: Any, b: Any) {}

fun `pass shared twice to shareBoth`(a: Any) {
    shareBoth(a, a)
}

fun `pass borrowed twice to borrowBoth`(a: @Borrowed Any) {
    borrowBoth(a, a)
}

fun `pass unique twice to consumeBoth`(a: @Unique Any) {
    // TODO: Report a collision for when a unique value is accessed twice in the same call expression
    consumeBoth(a, <!UNIQUENESS_MISMATCH!>a<!>)
}
