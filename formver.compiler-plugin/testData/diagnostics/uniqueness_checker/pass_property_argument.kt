// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

abstract class A {
    @Unique abstract var x: Any
    @Unique abstract var w: Any
}

abstract class B {
    @Unique abstract var y: A
}

fun borrowBoth(@Borrowed a: Any, @Borrowed b: Any) {}

fun consumeBoth(@Unique a: Any, @Unique b: Any) {}

fun shareBoth(a: Any, b: Any) {}

fun `pass shared subproperty and parent to shareBoth`(a: B) {
    shareBoth(a.y, a)
}

fun `pass borrowed subproperty and parent to borrowBoth`(@Borrowed a: B) {
    borrowBoth(a.y, a)
}

fun `pass unique subproperty and parent to consumeBoth`(@Unique a: B) {
    consumeBoth(a.y, <!UNIQUENESS_VIOLATION!>a<!>)
}

fun `pass unique-borrowed subproperty and parent to borrowBoth`(@Unique @Borrowed a: B) {
    borrowBoth(a.y, <!UNIQUENESS_VIOLATION!>a<!>)
}
