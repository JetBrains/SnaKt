// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

fun borrowBoth(@Borrowed a: Any, @Borrowed b: Any) {}

fun consumeBoth(@Unique a: Any, @Unique b: Any) {}

fun shareBoth(a: Any, b: Any) {}

fun `pass shared twice to shareBoth`(a: Any) {
    shareBoth(a, a)
}

fun `pass borrowed twice to borrowBoth`(@Borrowed a: Any) {
    borrowBoth(a, <!UNIQUENESS_VIOLATION!>a<!>)
}

fun `pass unique twice to consumeBoth`(@Unique a: Any) {
    consumeBoth(a, <!UNIQUENESS_VIOLATION!>a<!>)
}
