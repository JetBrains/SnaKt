// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.Borrowed

class A()

fun share(a: Any) {}

// Sharing locals

fun `share shared`(y: A) {
    share(y)
}

fun `share borrowed`(@Borrowed y: A) {
    share(<!UNIQUENESS_VIOLATION!>y<!>)
}

fun `share unique`(@Unique y: A) {
    share(y)
}

fun `share unique-borrowed`(@Borrowed @Unique y: A) {
    share(<!UNIQUENESS_VIOLATION!>y<!>)
}
