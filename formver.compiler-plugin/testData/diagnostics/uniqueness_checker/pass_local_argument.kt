// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

fun borrowBoth(a: @Borrowed Any, b: @Borrowed Any) {}

fun consumeBoth(a: @Unique Any, b: @Unique Any) {}

fun shareBoth(a: Any, b: Any) {}

fun `pass unique twice to consumeBoth`(a: @Unique Any) {
    // TODO: Report a collision for when a unique value is accessed twice in the same call expression
    consumeBoth(a, <!UNIQUENESS_MISMATCH!>a<!>)
}
