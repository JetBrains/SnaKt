// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

abstract class B {
    @Unique abstract var y: Throwable
}

fun `throw shared subproperty`(a: B) {
    throw a.y
}

fun `throw borrowed subproperty`(@Borrowed a: B) {
    <!UNIQUENESS_VIOLATION!>throw a.y<!>
}

fun `throw unique subproperty`(@Unique a: B) {
    throw a.y
}

fun `throw unique-borrowed subproperty`(@Unique @Borrowed a: B) {
    <!UNIQUENESS_VIOLATION!>throw a.y<!>
}
