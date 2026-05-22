// FULL_JDK
// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class B {
    var y: @Unique Exception = Exception()
}

fun `throw shared subproperty`(a: B) {
    throw a.y
}

fun `throw borrowed subproperty`(a: @Borrowed B) {
    throw a.y
}

fun `throw unique subproperty`(a: @Unique B) {
    throw a.y
}

fun `throw unique-borrowed subproperty`(a: @Unique @Borrowed B) {
    throw a.y
}
