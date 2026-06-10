// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A(
    var x: @Unique Any,
)

fun `assign local to local in if`(a: @Unique A) {
    val x: @Unique Any = if (false) {
        val x: @Unique Any = a.x
        x
    } else {
        a.x
    }
}

class B(
    var x: @Unique A,
)

class C(
    var x: @Unique B,
)

fun `assign property to local in if`(c: @Unique C) {
    val x = (if (false) {
        val y: @Unique A = c.x.x
        y
    } else {
        c.x.x
    }).x
}
