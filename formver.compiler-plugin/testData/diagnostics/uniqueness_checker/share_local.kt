// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A()

fun share(a: Any) {}

// Sharing locals

fun `share shared`(y: A) {
    share(y)
}

fun `share borrowed`(y: @Borrowed A) {
    share(<!LOCALITY_MISMATCH!>y<!>)
}

fun `share unique`(y: @Unique A) {
    share(y)
}

fun `share unique-borrowed`(y: @Borrowed @Unique A) {
    share(<!LOCALITY_MISMATCH!>y<!>)
}
