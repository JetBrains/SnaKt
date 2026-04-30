// FULL_JDK
// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class B {
    @Unique var y = Exception()
}

fun `throw shared subproperty`(a: B) {
    throw a.y
}

fun `throw borrowed subproperty`(@Borrowed a: B) {
    throw <!UNIQUENESS_VIOLATION!>a.y<!>
}

fun `throw unique subproperty`(@Unique a: B) {
    throw a.y
}

fun `throw unique-borrowed subproperty`(@Unique @Borrowed a: B) {
    throw <!UNIQUENESS_VIOLATION!>a.y<!>
}
