// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    var x: @Unique Any = Any()
    var w: @Unique Any = Any()
}

class B {
    var y: @Unique A = A()
}

fun borrowBoth(a: @Borrowed Any, b: @Borrowed Any) {}

fun consumeBoth(a: @Unique Any, b: @Unique Any) {}

fun shareBoth(a: Any, b: Any) {}

// TODO: Define error if a unique path is passed twice
fun `pass shared subproperty and parent to shareBoth`(a: B) {
    shareBoth(a.y, a)
}

fun `pass borrowed subproperty and parent to borrowBoth`(a: @Borrowed B) {
    borrowBoth(a.y, a)
}

fun `pass unique subproperty and parent to consumeBoth`(a: @Unique B) {
    consumeBoth(a.y, <!LEAKED_UNIQUENESS_CONSISTENCY_VIOLATION!>a<!>)
}

fun `pass unique-borrowed subproperty and parent to borrowBoth`(a: @Unique @Borrowed B) {
    borrowBoth(a.y, <!LEAKED_UNIQUENESS_CONSISTENCY_VIOLATION!>a<!>)
}
